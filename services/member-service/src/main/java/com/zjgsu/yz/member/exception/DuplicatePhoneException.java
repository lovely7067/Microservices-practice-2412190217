package com.zjgsu.yz.member.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 手机号已被注册（业务规则冲突，映射为 HTTP 409）。
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicatePhoneException extends RuntimeException {

	public DuplicatePhoneException(String phone) {
		super("手机号已被注册: " + phone);
	}
}
