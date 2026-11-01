package com.zjgsu.yz.checkin.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 依赖的远程服务不可用（映射为 HTTP 503）。
 * 出现该异常时请求终止，绝不跳过校验继续写入本地数据。
 */
@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class RemoteServiceException extends RuntimeException {

	public RemoteServiceException(String message) {
		super(message);
	}
}
