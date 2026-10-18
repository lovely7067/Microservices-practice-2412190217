package com.zjgsu.yz.gym.model;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * 会员卡实体：会员持有的卡务信息，是收费与入场权限的核心依据。
 */
@Entity
@Table(name = "membership_card")
public class MembershipCard {

	/** 主键，自增 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

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

	/** 所属会员（N — 1） */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	public Long getId() {
		return id;
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

	public Member getMember() {
		return member;
	}

	public void setMember(Member member) {
		this.member = member;
	}
}
