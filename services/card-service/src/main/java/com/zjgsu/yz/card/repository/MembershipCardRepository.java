package com.zjgsu.yz.card.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zjgsu.yz.card.model.MembershipCard;

/**
 * 会员卡数据访问：card-service 只读写自己的 membership_card 表。
 */
public interface MembershipCardRepository extends JpaRepository<MembershipCard, Long> {

	/** 查询某会员名下的全部会员卡（按保存的 member_id 列查询，非跨库关联） */
	List<MembershipCard> findByMemberId(Long memberId);
}
