package com.zjgsu.yz.card.web.dto;

import java.time.LocalDate;

import com.zjgsu.yz.card.model.CardStatus;
import com.zjgsu.yz.card.model.CardType;
import com.zjgsu.yz.card.model.MembershipCard;

/**
 * 会员卡响应 DTO（不直接暴露实体）。
 */
public class CardResponse {

	private Long id;
	private Long memberId;
	private CardType cardType;
	private LocalDate startDate;
	private LocalDate endDate;
	private Integer remainingTimes;
	private CardStatus status;

	public static CardResponse of(MembershipCard card) {
		CardResponse resp = new CardResponse();
		resp.id = card.getId();
		resp.memberId = card.getMemberId();
		resp.cardType = card.getCardType();
		resp.startDate = card.getStartDate();
		resp.endDate = card.getEndDate();
		resp.remainingTimes = card.getRemainingTimes();
		resp.status = card.getStatus();
		return resp;
	}

	public Long getId() {
		return id;
	}

	public Long getMemberId() {
		return memberId;
	}

	public CardType getCardType() {
		return cardType;
	}

	public LocalDate getStartDate() {
		return startDate;
	}

	public LocalDate getEndDate() {
		return endDate;
	}

	public Integer getRemainingTimes() {
		return remainingTimes;
	}

	public CardStatus getStatus() {
		return status;
	}
}
