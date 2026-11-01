package com.zjgsu.yz.checkin.web.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 入场核验请求 DTO。
 */
public class CreateCheckinRequest {

	/** 会员 ID */
	@NotNull(message = "会员 ID 不能为空")
	private Long memberId;

	/** 会员卡 ID（入场前会通过 HTTP 调用 card-service 校验卡片有效性） */
	@NotNull(message = "会员卡 ID 不能为空")
	private Long cardId;

	public Long getMemberId() {
		return memberId;
	}

	public void setMemberId(Long memberId) {
		this.memberId = memberId;
	}

	public Long getCardId() {
		return cardId;
	}

	public void setCardId(Long cardId) {
		this.cardId = cardId;
	}
}
