package com.zjgsu.yz.card.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate 配置：为跨服务 HTTP 调用设置连接与读取超时（可在 application.yml / 环境变量中调整）。
 */
@Configuration
public class RestTemplateConfig {

	@Bean
	public RestTemplate restTemplate(
			@Value("${member-service.connect-timeout-ms:2000}") int connectTimeoutMs,
			@Value("${member-service.read-timeout-ms:3000}") int readTimeoutMs) {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(connectTimeoutMs);
		factory.setReadTimeout(readTimeoutMs);
		return new RestTemplate(factory);
	}
}
