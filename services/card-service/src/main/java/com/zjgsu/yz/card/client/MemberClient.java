package com.zjgsu.yz.card.client;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.zjgsu.yz.card.exception.NotFoundException;
import com.zjgsu.yz.card.exception.RemoteServiceException;
import com.zjgsu.yz.card.web.dto.MemberDto;

/**
 * member-service 的调用客户端。
 * 本周改为服务发现方式：不再保存固定地址，每次调用前通过 DiscoveryClient
 * 按服务名 member-service（与对方 spring.application.name 一致）解析当前实例，
 * 再用实例的 URI 和原业务路径发送 HTTP 请求。
 * - 会员不存在（404）→ 抛出 NotFoundException
 * - 实例列表为空 / 请求失败 / 超时 → 抛出 RemoteServiceException（503），由调用方中止请求
 */
@Component
public class MemberClient {

	/** 目标服务名：与 member-service 的 spring.application.name 一致 */
	public static final String MEMBER_SERVICE_ID = "member-service";

	private final RestTemplate restTemplate;
	private final DiscoveryInstanceResolver instanceResolver;

	public MemberClient(RestTemplate restTemplate, DiscoveryInstanceResolver instanceResolver) {
		this.restTemplate = restTemplate;
		this.instanceResolver = instanceResolver;
	}

	public MemberDto getMember(Long memberId) {
		// 每次操作重新解析实例，不在启动时读取一次并永久保存地址
		URI baseUri = instanceResolver.resolve(MEMBER_SERVICE_ID);
		String url = baseUri + "/api/members/" + memberId;
		try {
			ResponseEntity<MemberDto> response = restTemplate.getForEntity(url, MemberDto.class);
			if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
				return response.getBody();
			}
			throw new NotFoundException("会员不存在: id=" + memberId);
		} catch (HttpClientErrorException.NotFound e) {
			throw new NotFoundException("会员不存在: id=" + memberId);
		} catch (RestClientException e) {
			throw new RemoteServiceException("member-service 调用失败（" + e.getMessage() + "），无法校验会员");
		}
	}
}
