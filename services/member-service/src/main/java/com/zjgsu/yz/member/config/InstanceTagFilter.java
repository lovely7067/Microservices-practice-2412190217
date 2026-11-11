package com.zjgsu.yz.member.config;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 实例标识请求日志（第 08 周：负载均衡验证证据）。
 *
 * <p>每个 HTTP 请求打印一行 {@code GYM-INSTANCE <实例名> <方法> <路径>}。
 * 实例名来自环境变量 {@code INSTANCE_NAME}（compose 中 member-service 为 member-1、
 * member-service-2 为 member-2），本地开发默认 member-local。</p>
 *
 * <p>用途：被调服务启动多实例后，调用方经 OpenFeign + LoadBalancer 轮询访问时，
 * 两个实例的容器日志中都会出现请求记录——以此证明调用方实际访问到了不同实例，
 * 而不是只看 Nacos 注册列表。</p>
 */
public class InstanceTagFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(InstanceTagFilter.class);

	private final String instanceName;

	public InstanceTagFilter() {
		this.instanceName = System.getenv().getOrDefault("INSTANCE_NAME", "member-local");
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		log.info("GYM-INSTANCE {} {} {}", instanceName, request.getMethod(), request.getRequestURI());
		filterChain.doFilter(request, response);
	}
}