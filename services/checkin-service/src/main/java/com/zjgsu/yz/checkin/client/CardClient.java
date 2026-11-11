package com.zjgsu.yz.checkin.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.zjgsu.yz.checkin.config.FeignErrorConfig;
import com.zjgsu.yz.checkin.web.dto.CardDto;

/**
 * card-service 的 OpenFeign 客户端（第 08 周）。
 *
 * <p>使用服务名调用而不是写死容器 IP：{@code name} 即被调方在 Nacos 注册的
 * {@code spring.application.name}，多实例经 Spring Cloud LoadBalancer 轮询分发。</p>
 *
 * <p>失败处理：HTTP 错误经 {@link FeignErrorConfig} 解码——
 * 404 → {@code NotFoundException}（卡不存在），其余 → {@code RemoteServiceException}（503）；
 * 无可用实例等本地异常由 GlobalExceptionHandler 统一映射为 503。
 * 验卡失败时入场流程中止，绝不写入入场记录。</p>
 */
@FeignClient(name = "card-service", path = "/api/cards", configuration = FeignErrorConfig.class)
public interface CardClient {

	/**
	 * 按 ID 查询会员卡（入场前必须验卡：本人卡、有效期、状态、剩余次数）。
	 *
	 * @param cardId 会员卡 ID
	 * @return 会员卡信息；卡不存在时抛出 NotFoundException（404）
	 */
	@GetMapping("/{id}")
	CardDto getCard(@PathVariable("id") Long cardId);
}
