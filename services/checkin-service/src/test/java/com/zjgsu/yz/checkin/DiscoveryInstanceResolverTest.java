package com.zjgsu.yz.checkin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import com.zjgsu.yz.checkin.client.CardClient;
import com.zjgsu.yz.checkin.client.DiscoveryInstanceResolver;
import com.zjgsu.yz.checkin.exception.RemoteServiceException;

/**
 * 实例解析测试：模拟 DiscoveryClient，不连接真实 Nacos。
 * 重点覆盖"实例列表为空"的场景：必须明确抛出依赖不可用错误，而不是继续请求。
 */
class DiscoveryInstanceResolverTest {

	private DiscoveryClient discoveryClient;

	private DiscoveryInstanceResolver resolver;

	@BeforeEach
	void setUp() {
		discoveryClient = mock(DiscoveryClient.class);
		resolver = new DiscoveryInstanceResolver(discoveryClient);
	}

	@Test
	void 实例列表为空时抛出依赖不可用错误() {
		when(discoveryClient.getInstances(CardClient.CARD_SERVICE_ID)).thenReturn(List.of());
		assertThrows(RemoteServiceException.class,
				() -> resolver.resolve(CardClient.CARD_SERVICE_ID));
	}

	@Test
	void 有实例时取第一个实例的URI() {
		when(discoveryClient.getInstances(CardClient.CARD_SERVICE_ID)).thenReturn(List.of(
				new DefaultServiceInstance("card-1", "card-service", "192.168.20.10", 8082, false),
				new DefaultServiceInstance("card-2", "card-service", "192.168.20.11", 8082, false)));
		URI uri = resolver.resolve(CardClient.CARD_SERVICE_ID);
		assertEquals(URI.create("http://192.168.20.10:8082"), uri);
	}
}
