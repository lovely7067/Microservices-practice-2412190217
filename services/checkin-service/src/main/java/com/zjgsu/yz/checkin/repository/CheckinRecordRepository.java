package com.zjgsu.yz.checkin.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zjgsu.yz.checkin.model.CheckinRecord;

/**
 * 入场记录数据访问：checkin-service 只读写自己的 checkin_record 表。
 */
public interface CheckinRecordRepository extends JpaRepository<CheckinRecord, Long> {

	/** 查询某会员的全部入场记录 */
	List<CheckinRecord> findByMemberId(Long memberId);
}
