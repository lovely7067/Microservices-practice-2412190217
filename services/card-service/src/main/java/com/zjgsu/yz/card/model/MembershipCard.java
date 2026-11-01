package com.zjgsu.yz.card.model;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * 会员卡实体：card-service 独享的数据表。
 * 边界规则：跨服务关联只保存会员 ID（member_id 为普通列 + 索引），
 * 不建立跨库外键，也不持有 member-service 的实体对象。
 */
@Entity
@Table(name = "membership_card", indexes = @Index(name = "idx_membership_card_member_id", columnList = "member_id"))
public class MembershipCard {

	/** 主键，自增 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** 所属会员 ID（跨服务引用，仅保存 ID，不建外键） */
	@Column(name = "member_id", nullable = false)
	private Long memberId;

	/** 卡类型：次卡 / 月卡 / 年卡 */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private CardType cardType;

	/** 有效期开始日期 */
	@Column(nullable = false)
	private LocalDate startDate;

	/** 有效期结束日期（业务规则：必须晚于开始日期） */
	@Column(nullable = false)
	private LocalDate endDate;

	/** 剩余次数（次卡适用，业务规则：次卡必须大于 0） */
	private Integer remainingTimes;

	/** 卡状态，默认正常 */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private CardStatus status = CardStatus.ACTIVE;

	public Long getId() {
		return id;
	}

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

	public CardStatus getStatus() {
		return status;
	}

	public void setStatus(CardStatus status) {
		this.status = status;
	}
}
