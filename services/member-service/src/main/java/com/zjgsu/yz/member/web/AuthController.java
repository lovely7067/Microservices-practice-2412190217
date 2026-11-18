package com.zjgsu.yz.member.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zjgsu.yz.member.service.AuthService;
import com.zjgsu.yz.member.web.dto.LoginRequest;
import com.zjgsu.yz.member.web.dto.LoginResponse;

import jakarta.validation.Valid;

/**
 * 认证接口（第 09 周）：登录换 JWT。该路径在网关侧为白名单（无需 Token）。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	/** 登录：返回 JWT 与角色信息 */
	@PostMapping("/login")
	public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
		return ResponseEntity.ok(authService.login(request));
	}
}
