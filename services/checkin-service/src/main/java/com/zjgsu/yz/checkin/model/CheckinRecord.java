package com.zjgsu.yz.checkin.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * 入场记录实体：checkin-service 独享的数据表。
 * 边界规则：member_id / card_id 都是跨服务引用的普通列（只保存对方 ID），
 * 不建立跨库外键，也不持有 card-service / member-service 的实体对象。
 */
@Entity
@Table(name = "checkin_record", indexes = {
		@Index(name = "idx_checkin_member_id", columnList = "member_id"),
		@Index(name = "idx_checkin_card_id", columnList = "card_id")
})
public class CheckinRecord {

	/** 主键，自增 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** 会员 ID（跨服务引用，仅保存 ID） */
	@Column(name = "member_id", nullable = false)
	private Long memberId;

	/** 会员卡 ID（跨服务引用，仅保存 ID） */
	@Column(name = "card_id", nullable = false)
	private Long cardId;

	/** 入场时间 */
	@Column(nullable = false)
	private LocalDateTime checkinTime;

	/** 是否允许入场（核验结果） */
	@Column(nullable = false)
	private Boolean allowed = true;

	public Long getId() {
		return id;
	}

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

	public LocalDateTime getCheckinTime() {
		return checkinTime;
	}

	public void setCheckinTime(LocalDateTime checkinTime) {
		this.checkinTime = checkinTime;
	}

	public Boolean getAllowed() {
		return allowed;
	}

	public void setAllowed(Boolean allowed) {
		this.allowed = allowed;
	}
}
