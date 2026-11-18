package com.zjgsu.yz.gateway.config;

import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import reactor.core.publisher.Mono;

/**
 * 网关全局鉴权过滤器（第 09 周）。
 *
 * <p>职责：
 * ① 白名单（登录、健康检查）直接放行；
 * ② 未携带 / 携带无效 JWT → 401；
 * ③ USER 角色访问管理接口 /api/members/statistics → 403；
 * ④ 验签通过后，剥离客户端伪造的身份请求头（X-User-Id / X-User-Role / X-User-Name / X-Gateway-Auth），
 *    再注入由 Token 解出的可信身份与网关签名头（X-Gateway-Auth），
 *    下游服务据其校验"请求确实经过网关"，防止绕过网关直连。</p>
 */
@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

	/** 网关注入给下游的签名头：值与 JWT_SECRET 一致，客户端不可预知 */
	public static final String HEADER_GATEWAY_AUTH = "X-Gateway-Auth";
	public static final String HEADER_USER_ID = "X-User-Id";
	public static final String HEADER_USER_ROLE = "X-User-Role";

	/** 无需 Token 的路径（登录与健康检查入口） */
	static final List<String> WHITELIST = List.of("/api/auth/login", "/api/health", "/actuator");

	/** 需要 ADMIN 角色的管理接口 */
	static final String ADMIN_ONLY_PATH = "/api/members/statistics";

	/** 客户端可能伪造的身份头：验签后一律剥离重注入 */
	static final List<String> STRIP_HEADERS =
			List.of("X-User-Id", "X-User-Role", "X-User-Name", "X-Gateway-Auth");

	private final SecretKey key;
	private final String jwtSecret;

	public JwtAuthFilter(@Value("${app.jwt.secret}") String jwtSecret) {
		this.jwtSecret = jwtSecret;
		this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
	}

	@Override
	public int getOrder() {
		// 在路由转发之前完成鉴权
		return -100;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		String path = exchange.getRequest().getURI().getPath();

		// ① 白名单直接放行
		if (isWhitelisted(path)) {
			return chain.filter(exchange);
		}

		// ② 校验 Bearer Token
		String auth = exchange.getRequest().getHeaders().getFirst("Authorization");
		if (auth == null || !auth.startsWith("Bearer ")) {
			return respond(exchange, HttpStatus.UNAUTHORIZED, "未登录：缺少 Authorization: Bearer <token> 请求头");
		}
		String token = auth.substring("Bearer ".length()).trim();

		Claims claims;
		try {
			claims = Jwts.parser().verifyWith(key).requireIssuer("member-service")
					.build().parseSignedClaims(token).getPayload();
		} catch (JwtException | IllegalArgumentException e) {
			return respond(exchange, HttpStatus.UNAUTHORIZED, "Token 无效或已过期");
		}

		String role = claims.get("role", String.class);

		// ③ 管理接口的角色校验
		if (path.startsWith(ADMIN_ONLY_PATH) && !"ADMIN".equals(role)) {
			return respond(exchange, HttpStatus.FORBIDDEN, "权限不足：该接口需要 ADMIN 角色");
		}

		// ④ 剥离客户端伪造的身份头，注入网关可信身份
		ServerHttpRequest.Builder builder = exchange.getRequest().mutate();
		builder.headers(headers -> STRIP_HEADERS.forEach(headers::remove));
		Object uid = claims.get("uid");
		builder.header(HEADER_USER_ID, uid == null ? "" : String.valueOf(uid));
		builder.header(HEADER_USER_ROLE, role == null ? "" : role);
		builder.header(HEADER_GATEWAY_AUTH, jwtSecret);
		ServerHttpRequest mutated = builder.build();

		return chain.filter(exchange.mutate().request(mutated).build());
	}

	static boolean isWhitelisted(String path) {
		return WHITELIST.stream().anyMatch(path::startsWith);
	}

	/** 返回结构化 JSON 错误（认证脚本据此断言） */
	private Mono<Void> respond(ServerWebExchange exchange, HttpStatus status, String message) {
		ServerHttpResponse response = exchange.getResponse();
		response.setStatusCode(status);
		response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
		String body = "{\"status\":" + status.value()
				+ ",\"error\":\"" + status.getReasonPhrase().toLowerCase().replace(' ', '-')
				+ "\",\"message\":\"" + message + "\"}";
		DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
		return response.writeWith(Mono.just(buffer));
	}
}
