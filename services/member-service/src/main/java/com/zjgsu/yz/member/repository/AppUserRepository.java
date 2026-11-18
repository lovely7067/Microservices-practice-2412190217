package com.zjgsu.yz.member.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zjgsu.yz.member.model.AppUser;

/**
 * 登录用户数据访问：app_user 表，与 member 表同库但互不关联。
 */
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

	/** 按登录名查找（登录接口用） */
	Optional<AppUser> findByUsername(String username);
}
