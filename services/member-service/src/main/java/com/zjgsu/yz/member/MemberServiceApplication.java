package com.zjgsu.yz.member;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;

import com.zjgsu.yz.member.config.InstanceTagFilter;

/**
 * 会员档案服务启动类。
 *
 * <p>第 08 周注册 {@link InstanceTagFilter}：每个请求打印实例标识日志，
 * 作为调用方经 OpenFeign + LoadBalancer 轮询访问多实例的验证证据。</p>
 */
@SpringBootApplication
public class MemberServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(MemberServiceApplication.class, args);
	}

	@Bean
	public FilterRegistrationBean<InstanceTagFilter> instanceTagFilter() {
		FilterRegistrationBean<InstanceTagFilter> registration = new FilterRegistrationBean<>(new InstanceTagFilter());
		registration.addUrlPatterns("/api/*");
		registration.setOrder(1);
		return registration;
	}
}
