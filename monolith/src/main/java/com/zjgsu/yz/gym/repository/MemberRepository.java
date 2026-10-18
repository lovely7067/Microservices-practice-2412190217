package com.zjgsu.yz.gym.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zjgsu.yz.gym.model.Member;

/**
 * 会员数据访问仓库。
 */
public interface MemberRepository extends JpaRepository<Member, Long> {

	/** 按手机号查询会员 */
	Optional<Member> findByPhone(String phone);

	/** 判断手机号是否已注册（业务规则：手机号唯一） */
	boolean existsByPhone(String phone);
}
