package com.zjgsu.yz.checkin.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.zjgsu.yz.checkin.exception.NotFoundException;
import com.zjgsu.yz.checkin.exception.RemoteServiceException;

import feign.Response;
import feign.Retryer;
import feign.codec.ErrorDecoder;

/**
 * OpenFeign 错误解码配置（第 08 周）。
 *
 * <p>把被调方（card-service）的 HTTP 错误码翻译为本地业务异常：</p>
 * <ul>
 *   <li>404 → {@code NotFoundException}：卡不存在，属业务错误，原样透传给入场请求方；</li>
 *   <li>其余 → {@code RemoteServiceException}：映射为 503，入场中止，绝不写入入场记录。</li>
 * </ul>
 * <p>关闭 Feign 默认重试：入场是写操作，失败后由调用方明确报错；超时后结果可能在
 * 远端已生效（未知状态），不做自动重试，按记录幂等核对。</p>
 */
@Configuration
public class FeignErrorConfig {

	@Bean
	public ErrorDecoder feignErrorDecoder() {
		return (String methodKey, Response response) -> {
			int status = response.status();
			if (status == 404) {
				return new NotFoundException(
						"card-service 返回 404：会员卡不存在（" + methodKey + "）");
			}
			return new RemoteServiceException(
					"card-service 调用失败：HTTP " + status + "（" + methodKey + "）");
		};
	}

	@Bean
	public Retryer feignRetryer() {
		return Retryer.NEVER_RETRY;
	}
}
