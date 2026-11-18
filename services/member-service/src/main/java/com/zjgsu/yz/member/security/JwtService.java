package com.zjgsu.yz.member.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.zjgsu.yz.member.model.AppUser;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * JWT 签发与验签（第 09 周）。
 *
 * <p>HS256 对称签名：member-service（登录签发）与 gateway-service（网关验签）
 * 通过同一环境变量 JWT_SECRET 共享密钥。密钥绝不写死在代码里，
 * 真实值只放在本地 .env（已 gitignore），仓库里仅提交 .env.example 占位。</p>
 */
@Component
public class JwtService {

	public static final String ISSUER = "member-service";

	private final SecretKey key;
	private final long ttlMinutes;

	public JwtService(@Value("${app.jwt.secret}") String secret,
			@Value("${app.jwt.ttl-minutes:120}") long ttlMinutes) {
		this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		this.ttlMinutes = ttlMinutes;
	}

	/** 为登录用户签发 JWT：sub=用户名，role=角色，2 小时过期 */
	public String issue(AppUser user) {
		Instant now = Instant.now();
		return Jwts.builder()
				.subject(user.getUsername())
				.claim("uid", user.getId())
				.claim("role", user.getRole().name())
				.issuer(ISSUER)
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plus(ttlMinutes, ChronoUnit.MINUTES)))
				.signWith(key)
				.compact();
	}

	/** 验签并解析 Claims；签名不符 / 过期 / 格式错误抛 JwtException */
	public Claims parse(String token) {
		return Jwts.parser()
				.verifyWith(key)
				.requireIssuer(ISSUER)
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}

	public long ttlMinutes() {
		return ttlMinutes;
	}
}
