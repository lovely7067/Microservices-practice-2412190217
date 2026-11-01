package com.zjgsu.yz.member.web.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.zjgsu.yz.member.model.Member;

/**
 * 会员响应 DTO（不直接暴露实体）。
 */
public class MemberResponse {

	private Long id;
	private String name;
	private String phone;
	private String gender;
	private LocalDate birthday;
	private LocalDateTime createdAt;

	public static MemberResponse of(Member member) {
		MemberResponse resp = new MemberResponse();
		resp.id = member.getId();
		resp.name = member.getName();
		resp.phone = member.getPhone();
		resp.gender = member.getGender();
		resp.birthday = member.getBirthday();
		resp.createdAt = member.getCreatedAt();
		return resp;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getPhone() {
		return phone;
	}

	public String getGender() {
		return gender;
	}

	public LocalDate getBirthday() {
		return birthday;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}
