package com.zjgsu.yz.card.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.zjgsu.yz.card.exception.NotFoundException;
import com.zjgsu.yz.card.exception.RemoteServiceException;
import com.zjgsu.yz.card.web.dto.MemberDto;

/**
 * member-service 的 HTTP 客户端：跨服务获取会员信息只能通过此 API 调用，
 * 不允许直接访问 member-service 的数据库或 Repository。
 */
@Component
public class MemberClient {

	private final RestTemplate restTemplate;
	private final String baseUrl;

	public MemberClient(RestTemplate restTemplate,
			@Value("${member-service.base-url}") String baseUrl) {
		this.restTemplate = restTemplate;
		this.baseUrl = baseUrl;
	}

	/**
	 * 按 ID 查询会员。
	 * - 会员不存在（404）→ 抛出 NotFoundException
	 * - member-service 不可达 / 超时 → 抛出 RemoteServiceException（503），由调用方决定中止请求
	 */
	public MemberDto getMember(Long memberId) {
		String url = baseUrl + "/api/members/" + memberId;
		try {
			ResponseEntity<MemberDto> response = restTemplate.getForEntity(url, MemberDto.class);
			if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
				return response.getBody();
			}
			throw new NotFoundException("会员不存在: id=" + memberId);
		} catch (HttpClientErrorException.NotFound e) {
			throw new NotFoundException("会员不存在: id=" + memberId);
		} catch (RestClientException e) {
			throw new RemoteServiceException("member-service 不可用（" + e.getMessage() + "），无法校验会员");
		}
	}
}
