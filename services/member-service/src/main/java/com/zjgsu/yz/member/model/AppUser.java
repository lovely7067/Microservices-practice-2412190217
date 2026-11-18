package com.zjgsu.yz.member.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 登录用户账号（第 09 周）：与业务实体 Member 分离，只负责认证。
 *
 * <p>密码只存 BCrypt 哈希（password_hash 列），绝不明文落库；
 * 角色两种：USER 普通用户、ADMIN 管理员。</p>
 */
@Entity
@Table(name = "app_user")
public class AppUser {

	/** 登录用户角色 */
	public enum Role {
		USER, ADMIN
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** 登录名，唯一 */
	@Column(nullable = false, unique = true, length = 50)
	private String username;

	/** BCrypt 密码哈希（60 字符），不存明文 */
	@Column(name = "password_hash", nullable = false, length = 100)
	private String passwordHash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Role role;

	public Long getId() {
		return id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public Role getRole() {
		return role;
	}

	public void setRole(Role role) {
		this.role = role;
	}
}
