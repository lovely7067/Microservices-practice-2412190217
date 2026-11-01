package com.zjgsu.yz.checkin.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zjgsu.yz.checkin.client.CardClient;
import com.zjgsu.yz.checkin.exception.InvalidRequestException;
import com.zjgsu.yz.checkin.model.CardStatus;
import com.zjgsu.yz.checkin.model.CheckinRecord;
import com.zjgsu.yz.checkin.repository.CheckinRecordRepository;
import com.zjgsu.yz.checkin.web.dto.CardDto;
import com.zjgsu.yz.checkin.web.dto.CreateCheckinRequest;

/**
 * 入场核验业务服务。
 * 跨服务规则：入场前必须通过 HTTP 调用 card-service 验卡；
 * card-service 不可用时返回明确错误，绝不跳过校验写入本地数据。
 */
@Service
public class CheckinService {

	private final CheckinRecordRepository checkinRepository;
	private final CardClient cardClient;

	public CheckinService(CheckinRecordRepository checkinRepository, CardClient cardClient) {
		this.checkinRepository = checkinRepository;
		this.cardClient = cardClient;
	}

	/**
	 * 入场核验。
	 * 业务规则 1（跨服务）：会员卡必须存在于 card-service（不存在 → 404，服务不可用 → 503）。
	 * 业务规则 2：会员卡必须属于该会员（卡上的 memberId 与请求一致）。
	 * 业务规则 3：会员卡状态必须为 ACTIVE 且未过期。
	 */
	@Transactional
	public CheckinRecord checkin(CreateCheckinRequest request) {
		// 跨服务校验：RemoteServiceException/NotFoundException 会直接中止流程
		CardDto card = cardClient.getCard(request.getCardId());

		if (!card.getMemberId().equals(request.getMemberId())) {
			throw new InvalidRequestException("会员卡不属于该会员: card.memberId=" + card.getMemberId());
		}
		if (card.getStatus() != CardStatus.ACTIVE) {
			throw new InvalidRequestException("会员卡当前状态不可入场: " + card.getStatus());
		}
		if (card.getEndDate() != null && !card.getEndDate().isAfter(LocalDate.now())) {
			throw new InvalidRequestException("会员卡已过期: " + card.getEndDate());
		}

		CheckinRecord record = new CheckinRecord();
		record.setMemberId(request.getMemberId());
		record.setCardId(request.getCardId());
		record.setCheckinTime(LocalDateTime.now());
		record.setAllowed(true);
		return checkinRepository.save(record);
	}

	/** 查询全部入场记录 */
	@Transactional(readOnly = true)
	public List<CheckinRecord> listCheckins() {
		return checkinRepository.findAll();
	}

	/** 查询某会员的入场记录 */
	@Transactional(readOnly = true)
	public List<CheckinRecord> listByMember(Long memberId) {
		return checkinRepository.findByMemberId(memberId);
	}
}
