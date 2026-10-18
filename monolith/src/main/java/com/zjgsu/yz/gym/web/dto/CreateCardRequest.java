package com.zjgsu.yz.gym.web.dto;

import java.time.LocalDate;

import com.zjgsu.yz.gym.model.CardType;

import jakarta.validation.constraints.NotNull;

/**
 * 办卡请求 DTO。
 */
public class CreateCardRequest {

	@NotNull(message = "卡类型不能为空")
	private CardType cardType;

	@NotNull(message = "开始日期不能为空")
	private LocalDate startDate;

	@NotNull(message = "结束日期不能为空")
	private LocalDate endDate;

	/** 剩余次数（次卡必填且大于 0，业务规则在 Service 中校验） */
	private Integer remainingTimes;

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
