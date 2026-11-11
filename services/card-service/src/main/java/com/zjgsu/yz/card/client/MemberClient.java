package com.zjgsu.yz.card.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.zjgsu.yz.card.config.FeignErrorConfig;
import com.zjgsu.yz.card.web.dto.MemberDto;

/**
 * member-service 的 OpenFeign 客户端（第 08 周）。
 *
 * <p>使用服务名调用而不是写死容器 IP：{@code name} 即被调方在 Nacos 注册的
 * {@code spring.application.name}，同一服务的多实例经 Spring Cloud LoadBalancer
 * 以轮询方式分发（负载均衡验证见 scripts/e2e-test.sh）。</p>
 *
 * <p>失败处理：HTTP 错误经 {@link FeignErrorConfig} 解码——
 * 404 → {@code NotFoundException}（业务不存在），其余 → {@code RemoteServiceException}（503）；
 * 无可用实例等本地异常由 GlobalExceptionHandler 统一映射为 503。
 * 校验失败时调用方中止流程，绝不写入本地数据。</p>
 */
@FeignClient(name = "member-service", path = "/api/members", configuration = FeignErrorConfig.class)
public interface MemberClient {

	/**
	 * 按 ID 查询会员（办卡前必须校验会员存在）。
	 *
	 * @param memberId 会员 ID
	 * @return 会员信息；会员不存在时抛出 NotFoundException（404）
	 */
	@GetMapping("/{id}")
	MemberDto getMember(@PathVariable("id") Long memberId);
}
