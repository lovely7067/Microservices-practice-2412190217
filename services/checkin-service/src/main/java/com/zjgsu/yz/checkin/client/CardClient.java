package com.zjgsu.yz.checkin.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.zjgsu.yz.checkin.exception.NotFoundException;
import com.zjgsu.yz.checkin.exception.RemoteServiceException;
import com.zjgsu.yz.checkin.web.dto.CardDto;

/**
 * card-service 的 HTTP 客户端：跨服务获取会员卡信息只能通过此 API 调用，
 * 不允许直接访问 card-service 的数据库或 Repository。
 */
@Component
public class CardClient {

	private final RestTemplate restTemplate;
	private final String baseUrl;

	public CardClient(RestTemplate restTemplate,
			@Value("${card-service.base-url}") String baseUrl) {
		this.restTemplate = restTemplate;
		this.baseUrl = baseUrl;
	}

	/**
	 * 按 ID 查询会员卡。
	 * - 卡不存在（404）→ 抛出 NotFoundException
	 * - card-service 不可达 / 超时 → 抛出 RemoteServiceException（503），由调用方决定中止请求
	 */
	public CardDto getCard(Long cardId) {
		String url = baseUrl + "/api/cards/" + cardId;
		try {
			ResponseEntity<CardDto> response = restTemplate.getForEntity(url, CardDto.class);
			if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
				return response.getBody();
			}
			throw new NotFoundException("会员卡不存在: id=" + cardId);
		} catch (HttpClientErrorException.NotFound e) {
			throw new NotFoundException("会员卡不存在: id=" + cardId);
		} catch (RestClientException e) {
			throw new RemoteServiceException("card-service 不可用（" + e.getMessage() + "），无法完成入场验卡");
		}
	}
}
