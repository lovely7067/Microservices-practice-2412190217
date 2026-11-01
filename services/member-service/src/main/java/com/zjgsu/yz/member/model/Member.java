package com.zjgsu.yz.member.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * 会员实体：会员档案服务独享的数据表，不含会员卡信息（卡片归 card-service 所有）。
 */
@Entity
@Table(name = "member")
public class Member {

	/** 主键，自增 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** 姓名，必填 */
	@Column(nullable = false, length = 50)
	private String name;

	/** 手机号，必填且本库唯一（业务规则：同一手机号只能注册一个会员） */
	@Column(nullable = false, unique = true, length = 20)
	private String phone;

	/** 性别 */
	@Column(length = 10)
	private String gender;

	/** 生日 */
	private LocalDate birthday;

	/** 入会时间，创建时自动记录 */
	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@PrePersist
	void onCreate() {
		this.createdAt = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getGender() {
		return gender;
	}

	public void setGender(String gender) {
		this.gender = gender;
	}

	public void setBirthday(LocalDate birthday) {
		this.birthday = birthday;
	}

	public LocalDate getBirthday() {
		return birthday;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}
