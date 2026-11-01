package com.zjgsu.yz.checkin.web.dto;

import java.time.LocalDateTime;

import com.zjgsu.yz.checkin.model.CheckinRecord;

/**
 * 入场记录响应 DTO（不直接暴露实体）。
 */
public class CheckinResponse {

	private Long id;
	private Long memberId;
	private Long cardId;
	private LocalDateTime checkinTime;
	private Boolean allowed;

	public static CheckinResponse of(CheckinRecord record) {
		CheckinResponse resp = new CheckinResponse();
		resp.id = record.getId();
		resp.memberId = record.getMemberId();
		resp.cardId = record.getCardId();
		resp.checkinTime = record.getCheckinTime();
		resp.allowed = record.getAllowed();
		return resp;
	}

	public Long getId() {
		return id;
	}

	public Long getMemberId() {
		return memberId;
	}

	public Long getCardId() {
		return cardId;
	}

	public LocalDateTime getCheckinTime() {
		return checkinTime;
	}

	public Boolean getAllowed() {
		return allowed;
	}
}
