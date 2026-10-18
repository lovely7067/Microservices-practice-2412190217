package com.zjgsu.yz.gym.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zjgsu.yz.gym.model.MembershipCard;

/**
 * 会员卡数据访问仓库。
 */
public interface MembershipCardRepository extends JpaRepository<MembershipCard, Long> {

	/** 查询某会员名下的全部会员卡 */
	List<MembershipCard> findByMemberId(Long memberId);
}
