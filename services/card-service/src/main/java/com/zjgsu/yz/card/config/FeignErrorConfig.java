package com.zjgsu.yz.card.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import feign.Response;
import feign.Retryer;
import feign.codec.ErrorDecoder;

/**
 * OpenFeign 错误解码配置（第 08 周）。
 *
 * <p>把被调方的 HTTP 错误码翻译为本地业务异常，调用方拿到的错误信息清晰且与
 * 直连实现（第 07 周）语义一致：</p>
 * <ul>
 *   <li>404 → {@code NotFoundException}：目标资源确实不存在，属业务错误，原样透传；</li>
 *   <li>其余（4xx/5xx）→ {@code RemoteServiceException}：映射为 503，
 *       调用方中止流程，绝不把调用失败当成业务成功。</li>
 * </ul>
 * <p>同时关闭 Feign 默认重试（Retryer.NEVER_RETRY）：写操作失败后由调用方明确报错，
 * 避免重复请求造成歧义；写操作超时后的结果可能在远端已生效，属于未知状态，
 * 由人工按记录幂等核对，不自动重试。</p>
 */
@Configuration
public class FeignErrorConfig {

	@Bean
	public ErrorDecoder feignErrorDecoder() {
		return (String methodKey, Response response) -> {
			int status = response.status();
			if (status == 404) {
				return new com.zjgsu.yz.card.exception.NotFoundException(
						"member-service 返回 404：目标会员不存在（" + methodKey + "）");
			}
			return new com.zjgsu.yz.card.exception.RemoteServiceException(
					"member-service 调用失败：HTTP " + status + "（" + methodKey + "）");
		};
	}

	@Bean
	public Retryer feignRetryer() {
		return Retryer.NEVER_RETRY;
	}
}
