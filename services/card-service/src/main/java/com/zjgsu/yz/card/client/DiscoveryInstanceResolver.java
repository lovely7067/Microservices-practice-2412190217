package com.zjgsu.yz.card.client;

import java.net.URI;
import java.util.List;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;

import com.zjgsu.yz.card.exception.RemoteServiceException;

/**
 * 从 Nacos 服务发现解析目标服务实例。
 * 规则：每次操作都通过 DiscoveryClient 获取当前已知实例（不在启动时读取一次并永久保存）；
 * 实例列表为空时明确返回依赖不可用错误（503）；本周只取第一个实例，不实现轮询、权重路由、重试或熔断。
 */
@Component
public class DiscoveryInstanceResolver {

	private final DiscoveryClient discoveryClient;

	public DiscoveryInstanceResolver(DiscoveryClient discoveryClient) {
		this.discoveryClient = discoveryClient;
	}

	/**
	 * 按服务名（与对方 spring.application.name 一致）解析一个可用实例的 URI。
	 */
	public URI resolve(String serviceId) {
		List<ServiceInstance> instances = discoveryClient.getInstances(serviceId);
		if (instances == null || instances.isEmpty()) {
			throw new RemoteServiceException("服务 " + serviceId + " 在 Nacos 中无可用实例，无法调用");
		}
		return instances.get(0).getUri();
	}
}
