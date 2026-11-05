package com.zjgsu.yz.card.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate 配置：跨服务调用保留连接与读取超时（可在 application.yml / 环境变量中调整）。
 * 目标实例地址不再来自固定配置，而是每次调用前通过 DiscoveryClient 解析。
 */
@Configuration
public class RestTemplateConfig {

	@Bean
	public RestTemplate restTemplate(
			@Value("${http-client.connect-timeout-ms:2000}") int connectTimeoutMs,
			@Value("${http-client.read-timeout-ms:3000}") int readTimeoutMs) {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(connectTimeoutMs);
		factory.setReadTimeout(readTimeoutMs);
		return new RestTemplate(factory);
	}
}
