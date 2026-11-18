package com.zjgsu.yz.member.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.zjgsu.yz.member.model.AppUser;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;

/**
 * JWT 签发与验签单元测试：签发后可解析、篡改签名报错、角色写进 claim。
 */
class JwtServiceTest {

	private static final String SECRET = "test-only-jwt-secret-for-unit-tests-32bytes";

	private JwtService jwtService;

	@BeforeEach
	void setUp() {
		jwtService = new JwtService(SECRET, 120);
	}

	private AppUser user() {
		AppUser user = new AppUser();
		user.setUsername("user1");
		user.setPasswordHash("bcrypt-hash");
		user.setRole(AppUser.Role.USER);
		return user;
	}

	@Test
	@DisplayName("签发的 JWT 可被同一密钥解析，包含用户名与角色")
	void issueThenParse() {
		String token = jwtService.issue(user());
		Claims claims = jwtService.parse(token);
		assertThat(claims.getSubject()).isEqualTo("user1");
		assertThat(claims.get("role", String.class)).isEqualTo("USER");
		assertThat(claims.getIssuer()).isEqualTo("member-service");
		assertThat(claims.getExpiration()).isAfter(Instant.now());
	}

	@Test
	@DisplayName("被篡改的 Token 验签失败")
	void tamperedTokenRejected() {
		String token = jwtService.issue(user()) + "x";
		assertThatThrownBy(() -> jwtService.parse(token)).isInstanceOf(Exception.class);
	}

	@Test
	@DisplayName("其他密钥签发的 Token 无法通过验签")
	void foreignKeyRejected() {
		SecretKey otherKey = Keys.hmacShaKeyFor("another-secret-key-with-32-bytes-min!!".getBytes(StandardCharsets.UTF_8));
		String foreign = Jwts.builder()
				.subject("user1")
				.claim("role", "ADMIN")
				.issuer("member-service")
				.issuedAt(new Date())
				.expiration(Date.from(Instant.now().plusSeconds(600)))
				.signWith(otherKey)
				.compact();
		assertThatThrownBy(() -> jwtService.parse(foreign)).isInstanceOf(Exception.class);
	}
}
