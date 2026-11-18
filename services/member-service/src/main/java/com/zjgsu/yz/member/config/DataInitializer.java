package com.zjgsu.yz.member.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.zjgsu.yz.member.model.AppUser;
import com.zjgsu.yz.member.repository.AppUserRepository;

/**
 * 测试账号初始化（第 09 周）：app_user 表为空时创建两个内置账号。
 *
 * <p>密码在启动时经 BCrypt 哈希后落库，数据库与代码中均无明文。
 * 测试账号信息（演示用，非真实密钥）记录在 README：admin/admin123（ADMIN）、user1/user123（USER）。</p>
 */
@Component
public class DataInitializer implements CommandLineRunner {

	private final AppUserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public DataInitializer(AppUserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	public void run(String... args) {
		if (userRepository.count() > 0) {
			return;
		}
		createUser("admin", "admin123", AppUser.Role.ADMIN);
		createUser("user1", "user123", AppUser.Role.USER);
	}

	private void createUser(String username, String rawPassword, AppUser.Role role) {
		AppUser user = new AppUser();
		user.setUsername(username);
		user.setPasswordHash(passwordEncoder.encode(rawPassword));
		user.setRole(role);
		userRepository.save(user);
	}
}
