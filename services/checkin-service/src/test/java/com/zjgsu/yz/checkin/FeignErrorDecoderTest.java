package com.zjgsu.yz.checkin;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.zjgsu.yz.checkin.config.FeignErrorConfig;
import com.zjgsu.yz.checkin.exception.NotFoundException;
import com.zjgsu.yz.checkin.exception.RemoteServiceException;

import feign.Request;
import feign.Response;
import feign.codec.ErrorDecoder;

/**
 * Feign 错误解码测试（第 08 周）：不发起真实 HTTP 请求，
 * 验证 card-service 的错误状态码被翻译为正确的本地业务异常——
 * 404 是业务错误（卡不存在，透传），其余是依赖故障（503，入场中止且不写记录）。
 */
class FeignErrorDecoderTest {

	private final ErrorDecoder decoder = new FeignErrorConfig().feignErrorDecoder();

	private Response responseOf(int status) {
		return Response.builder()
				.status(status)
				.reason("mock")
				.request(Request.create(Request.HttpMethod.GET, "http://card-service/api/cards/1",
						Collections.emptyMap(), null, null, null))
				.headers(Collections.emptyMap())
				.build();
	}

	@Test
	void 被调方404解码为业务级未找到异常() {
		Exception e = decoder.decode("CardClient#getCard(Long)", responseOf(404));
		assertInstanceOf(NotFoundException.class, e);
	}

	@Test
	void 被调方500解码为依赖不可用异常() {
		Exception e = decoder.decode("CardClient#getCard(Long)", responseOf(500));
		assertInstanceOf(RemoteServiceException.class, e);
	}

	@Test
	void 依赖不可用异常包含可读的状态码信息() {
		RemoteServiceException e = (RemoteServiceException) decoder.decode(
				"CardClient#getCard(Long)", responseOf(502));
		String message = e.getMessage();
		assert message.contains("502");
	}
}