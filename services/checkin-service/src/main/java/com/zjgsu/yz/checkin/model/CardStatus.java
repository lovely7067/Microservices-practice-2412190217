package com.zjgsu.yz.checkin.model;

/**
 * 会员卡状态（与 card-service 的枚举保持一致的数据契约）。
 */
public enum CardStatus {
	/** 正常 */
	ACTIVE,
	/** 已冻结 */
	FROZEN,
	/** 已过期 */
	EXPIRED
}
