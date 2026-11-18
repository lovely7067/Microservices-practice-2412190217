package com.zjgsu.yz.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import reactor.core.publisher.Mono;

/**
 * 网关鉴权过滤器单元测试：不启动 Spring 容器，直接构造请求断言。
 * 覆盖：白名单放行、未登录 401、坏 Token 401、有效 Token 放行并注入可信头、
 * 客户端伪造身份头被剥离、USER 访问管理接口 403。
 */
class JwtAuthFilterTest {

	private static final String SECRET = "test-only-jwt-secret-for-unit-tests-32bytes";

	private final SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
	private JwtAuthFilter filter;

	/** 捕获放行后的 exchange，用于断言转发给下游的请求头 */
	private final List<ServerWebExchange> chained = new CopyOnWriteArrayList<>();

	private final GatewayFilterChain chain = exchange -> {
		chained.add(exchange);
		return Mono.empty();
	};

	@BeforeEach
	void setUp() {
		filter = new JwtAuthFilter(SECRET);
		chained.clear();
	}

	private String token(String role, Long uid) {
		Instant now = Instant.now();
		return Jwts.builder()
				.subject("user1")
				.claim("uid", uid)
				.claim("role", role)
				.issuer("member-service")
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plusSeconds(600)))
				.signWith(key)
				.compact();
	}

	private MockServerWebExchange run(MockServerHttpRequest request) {
		MockServerWebExchange exchange = MockServerWebExchange.from(request);
		filter.filter(exchange, chain).block();
		return exchange;
	}

	@Test
	@DisplayName("白名单：登录与健康检查入口无需 Token")
	void whitelistPassesWithoutToken() {
		run(MockServerHttpRequest.post("/api/auth/login").build());
		run(MockServerHttpRequest.get("/api/health").build());
		run(MockServerHttpRequest.get("/actuator/health").build());
		assertThat(chained).hasSize(3);
	}

	@Test
	@DisplayName("未登录访问受保护接口 → 401")
	void missingTokenIs401() {
		MockServerWebExchange exchange = run(MockServerHttpRequest.get("/api/members").build());
		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		assertThat(chained).isEmpty();
	}

	@Test
	@DisplayName("坏 Token / 篡改签名 → 401")
	void badTokenIs401() {
		MockServerWebExchange tampered = run(MockServerHttpRequest.get("/api/members")
				.header("Authorization", "Bearer " + token("USER", 1L) + "x").build());
		assertThat(tampered.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

		MockServerWebExchange garbage = run(MockServerHttpRequest.get("/api/members")
				.header("Authorization", "Bearer not-a-jwt").build());
		assertThat(garbage.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	@DisplayName("有效 Token 放行，并注入网关可信身份头")
	void validTokenInjectsTrustedHeaders() {
		run(MockServerHttpRequest.get("/api/members")
				.header("Authorization", "Bearer " + token("USER", 7L)).build());
		assertThat(chained).hasSize(1);
		ServerWebExchange forwarded = chained.get(0);
		assertThat(forwarded.getRequest().getHeaders().getFirst(JwtAuthFilter.HEADER_USER_ID)).isEqualTo("7");
		assertThat(forwarded.getRequest().getHeaders().getFirst(JwtAuthFilter.HEADER_USER_ROLE)).isEqualTo("USER");
		assertThat(forwarded.getRequest().getHeaders().getFirst(JwtAuthFilter.HEADER_GATEWAY_AUTH)).isEqualTo(SECRET);
	}

	@Test
	@DisplayName("客户端伪造的身份请求头被剥离，无法冒充")
	void forgedHeadersAreStripped() {
		MockServerWebExchange exchange = run(MockServerHttpRequest.get("/api/members")
				.header("Authorization", "Bearer " + token("USER", 7L))
				.header("X-User-Role", "ADMIN")
				.header("X-User-Id", "999")
				.header("X-Gateway-Auth", "forged-secret")
				.build());
		assertThat(chained).hasSize(1);
		ServerWebExchange forwarded = chained.get(0);
		// 伪造头已被剥离重注入：角色来自 Token 而非请求头
		assertThat(forwarded.getRequest().getHeaders().getFirst(JwtAuthFilter.HEADER_USER_ROLE)).isEqualTo("USER");
		assertThat(forwarded.getRequest().getHeaders().getFirst(JwtAuthFilter.HEADER_USER_ID)).isEqualTo("7");
		assertThat(forwarded.getRequest().getHeaders().getFirst(JwtAuthFilter.HEADER_GATEWAY_AUTH)).isEqualTo(SECRET);
	}

	@Test
	@DisplayName("USER 访问管理接口 /api/members/statistics → 403")
	void userRoleOnAdminPathIs403() {
		MockServerWebExchange exchange = run(MockServerHttpRequest.get("/api/members/statistics")
				.header("Authorization", "Bearer " + token("USER", 7L))
				// 同时伪造角色头也无法越过网关校验：校验依据是 Token 内的 role
				.header("X-User-Role", "ADMIN")
				.build());
		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
		assertThat(chained).isEmpty();
	}

	@Test
	@DisplayName("ADMIN 访问管理接口 → 放行")
	void adminRoleOnAdminPathPasses() {
		run(MockServerHttpRequest.get("/api/members/statistics")
				.header("Authorization", "Bearer " + token("ADMIN", 1L)).build());
		assertThat(chained).hasSize(1);
	}
}
