package com.zjgsu.yz.card;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 会员卡服务启动类。
 *
 * <p>第 08 周启用 OpenFeign：MemberClient 以服务名 member-service 声明式调用，
 * 多实例经 LoadBalancer 轮询分发。</p>
 */
@SpringBootApplication
@EnableFeignClients
public class CardServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(CardServiceApplication.class, args);
	}
}
