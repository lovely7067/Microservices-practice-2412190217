package com.zjgsu.yz.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zjgsu.yz.member.model.Member;

/**
 * 会员数据访问：member-service 只读写自己的 member 表。
 */
public interface MemberRepository extends JpaRepository<Member, Long> {

	/** 手机号唯一性检查（业务规则 1） */
	boolean existsByPhone(String phone);
}
