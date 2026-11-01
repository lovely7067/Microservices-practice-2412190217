package com.zjgsu.yz.checkin.web.dto;

import com.zjgsu.yz.checkin.model.CardStatus;

/**
 * card-service 返回的会员卡信息（跨服务 API 的数据契约）。
 */
public class CardDto {

	private Long id;
	private Long memberId;
	private String cardType;
	private CardStatus status;
	private java.time.LocalDate endDate;
	private Integer remainingTimes;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getMemberId() {
		return memberId;
	}

	public void setMemberId(Long memberId) {
		this.memberId = memberId;
	}

	public String getCardType() {
		return cardType;
	}

	public void setCardType(String cardType) {
		this.cardType = cardType;
	}

	public CardStatus getStatus() {
		return status;
	}

	public void setStatus(CardStatus status) {
		this.status = status;
	}

	public java.time.LocalDate getEndDate() {
		return endDate;
	}

	public void setEndDate(java.time.LocalDate endDate) {
		this.endDate = endDate;
	}

	public Integer getRemainingTimes() {
		return remainingTimes;
	}

	public void setRemainingTimes(Integer remainingTimes) {
		this.remainingTimes = remainingTimes;
	}
}
