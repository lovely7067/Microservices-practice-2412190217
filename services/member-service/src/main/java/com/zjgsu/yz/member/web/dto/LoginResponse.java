package com.zjgsu.yz.member.web.dto;

/**
 * 登录响应体：JWT 与角色信息。
 */
public record LoginResponse(
		String token,
		String tokenType,
		long expiresIn,
		String role,
		String username) {
}
