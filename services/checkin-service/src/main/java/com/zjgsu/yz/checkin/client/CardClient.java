package com.zjgsu.yz.checkin.client;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.zjgsu.yz.checkin.exception.NotFoundException;
import com.zjgsu.yz.checkin.exception.RemoteServiceException;
import com.zjgsu.yz.checkin.web.dto.CardDto;

/**
 * card-service 的调用客户端。
 * 本周改为服务发现方式：不再保存固定地址，每次调用前通过 DiscoveryClient
 * 按服务名 card-service（与对方 spring.application.name 一致）解析当前实例，
 * 再用实例的 URI 和原业务路径发送 HTTP 请求。
 * - 卡不存在（404）→ 抛出 NotFoundException
 * - 实例列表为空 / 请求失败 / 超时 → 抛出 RemoteServiceException（503），由调用方中止请求
 */
@Component
public class CardClient {

	/** 目标服务名：与 card-service 的 spring.application.name 一致 */
	public static final String CARD_SERVICE_ID = "card-service";

	private final RestTemplate restTemplate;
	private final DiscoveryInstanceResolver instanceResolver;

	public CardClient(RestTemplate restTemplate, DiscoveryInstanceResolver instanceResolver) {
		this.restTemplate = restTemplate;
		this.instanceResolver = instanceResolver;
	}

	/**
	 * 按 ID 查询会员卡。
	 * - 卡不存在（404）→ 抛出 NotFoundException
	 * - 实例列表为空 / 请求失败 / 超时 → 抛出 RemoteServiceException（503），由调用方中止请求
	 */
	public CardDto getCard(Long cardId) {
		// 每次操作重新解析实例，不在启动时读取一次并永久保存地址
		URI baseUri = instanceResolver.resolve(CARD_SERVICE_ID);
		String url = baseUri + "/api/cards/" + cardId;
		try {
			ResponseEntity<CardDto> response = restTemplate.getForEntity(url, CardDto.class);
			if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
				return response.getBody();
			}
			throw new NotFoundException("会员卡不存在: id=" + cardId);
		} catch (HttpClientErrorException.NotFound e) {
			throw new NotFoundException("会员卡不存在: id=" + cardId);
		} catch (RestClientException e) {
			throw new RemoteServiceException("card-service 调用失败（" + e.getMessage() + "），无法完成入场验卡");
		}
	}
}

