package com.zjgsu.yz.member.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.zjgsu.yz.member.exception.InvalidCredentialsException;
import com.zjgsu.yz.member.model.AppUser;
import com.zjgsu.yz.member.repository.AppUserRepository;
import com.zjgsu.yz.member.security.JwtService;
import com.zjgsu.yz.member.web.dto.LoginRequest;
import com.zjgsu.yz.member.web.dto.LoginResponse;

/**
 * 认证服务（第 09 周）：校验用户名密码（BCrypt 比对），签发 JWT。
 */
@Service
public class AuthService {

	private final AppUserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;

	public AuthService(AppUserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
	}

	/**
	 * 登录：用户不存在或密码错误统一返回 401（不区分两种失败，避免枚举用户名）。
	 */
	public LoginResponse login(LoginRequest request) {
		AppUser user = userRepository.findByUsername(request.username().trim())
				.orElseThrow(AuthService::invalidCredentials);
		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw invalidCredentials();
		}
		String token = jwtService.issue(user);
		return new LoginResponse(token, "Bearer", jwtService.ttlMinutes() * 60,
				user.getRole().name(), user.getUsername());
	}

	private static InvalidCredentialsException invalidCredentials() {
		return new InvalidCredentialsException("用户名或密码错误");
	}
}
