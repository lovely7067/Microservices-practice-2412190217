package com.zjgsu.yz.card.web.dto;

import java.time.LocalDate;

import com.zjgsu.yz.card.model.CardType;

import jakarta.validation.constraints.NotNull;

/**
 * 办卡请求 DTO（memberId 为跨服务引用的会员 ID）。
 */
public class CreateCardRequest {

	/** 所属会员 ID（办卡前会通过 HTTP 调用 member-service 校验存在性） */
	@NotNull(message = "会员 ID 不能为空")
	private Long memberId;

	@NotNull(message = "卡类型不能为空")
	private CardType cardType;

	@NotNull(message = "开始日期不能为空")
	private LocalDate startDate;

	@NotNull(message = "结束日期不能为空")
	private LocalDate endDate;

	/** 剩余次数（次卡必填且大于 0，业务规则在 Service 中校验） */
	private Integer remainingTimes;

	public Long getMemberId() {
		return memberId;
	}

	public void setMemberId(Long memberId) {
		this.memberId = memberId;
	}

	public CardType getCardType() {
		return cardType;
	}

	public void setCardType(CardType cardType) {
		this.cardType = cardType;
	}

	public LocalDate getStartDate() {
		return startDate;
	}

	public void setStartDate(LocalDate startDate) {
		this.startDate = startDate;
	}

	public LocalDate getEndDate() {
		return endDate;
	}

	public void setEndDate(LocalDate endDate) {
		this.endDate = endDate;
	}

	public Integer getRemainingTimes() {
		return remainingTimes;
	}

	public void setRemainingTimes(Integer remainingTimes) {
		this.remainingTimes = remainingTimes;
	}
}
