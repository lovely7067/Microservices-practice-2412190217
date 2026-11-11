package com.zjgsu.yz.checkin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 入场核验服务启动类。
 *
 * <p>第 08 周启用 OpenFeign：CardClient 以服务名 card-service 声明式调用，
 * 多实例经 LoadBalancer 轮询分发。</p>
 */
@SpringBootApplication
@EnableFeignClients
public class CheckinServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(CheckinServiceApplication.class, args);
	}
}
