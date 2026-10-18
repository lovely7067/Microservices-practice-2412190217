package com.zjgsu.yz.gym.web.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 会员登记请求 DTO。
 */
public class CreateMemberRequest {

	@NotBlank(message = "姓名不能为空")
	@Size(max = 50, message = "姓名长度不能超过 50")
	private String name;

	@NotBlank(message = "手机号不能为空")
	@Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确（11 位数字，以 1 开头）")
	private String phone;

	/** 性别：男 / 女，可为空 */
	@Size(max = 10)
	private String gender;

	@NotNull(message = "生日不能为空")
	private LocalDate birthday;

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

	public LocalDate getBirthday() {
		return birthday;
	}

	public void setBirthday(LocalDate birthday) {
		this.birthday = birthday;
	}
}
