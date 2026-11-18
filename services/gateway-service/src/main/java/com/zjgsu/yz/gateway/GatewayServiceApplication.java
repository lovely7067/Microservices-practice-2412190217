package com.zjgsu.yz.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API 网关启动类（第 09 周）。
 *
 * <p>统一对外入口 8080：路由转发（目标为 Nacos 服务名）+ JWT 全局鉴权。
 * 基于 Spring Cloud Gateway（WebFlux 响应式）。</p>
 */
@SpringBootApplication
public class GatewayServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(GatewayServiceApplication.class, args);
	}
}
