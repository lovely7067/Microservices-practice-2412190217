package com.zjgsu.yz.card.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import feign.FeignException;

/**
 * 全局异常处理（第 08 周新增，覆盖 OpenFeign 场景）。
 *
 * <p>{@code NotFoundException} / {@code RemoteServiceException} 等 @ResponseStatus
 * 异常由 Spring 原生机制映射，不在本类重复处理；这里补齐 Feign 调用中的
 * <b>本地异常</b>（无 HTTP 响应、不走 ErrorDecoder），保证"依赖不可用"始终返回
 * 清晰的 503，而不是 500：</p>
 * <ul>
 *   <li>{@link FeignException}：LoadBalancer 无可用实例、连接拒绝等被 Feign 包装后抛出；</li>
 *   <li>{@link IllegalStateException}：Spring Cloud LoadBalancer 找不到实例时直接抛出
 *       （"No instances available for ..."）。</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(FeignException.class)
	@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
	public String handleFeign(FeignException e) {
		return "member-service 暂不可用（Feign " + e.status() + "），请求已中止，未写入任何数据";
	}

	@ExceptionHandler(IllegalStateException.class)
	@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
	public String handleNoInstance(IllegalStateException e) {
		return "member-service 无可用实例，请求已中止，未写入任何数据";
	}
}
