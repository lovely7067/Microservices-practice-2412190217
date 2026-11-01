package com.zjgsu.yz.card.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zjgsu.yz.card.client.MemberClient;
import com.zjgsu.yz.card.exception.InvalidRequestException;
import com.zjgsu.yz.card.exception.NotFoundException;
import com.zjgsu.yz.card.model.CardType;
import com.zjgsu.yz.card.model.MembershipCard;
import com.zjgsu.yz.card.repository.MembershipCardRepository;
import com.zjgsu.yz.card.web.dto.CreateCardRequest;
import com.zjgsu.yz.card.web.dto.MemberDto;

/**
 * 会员卡业务服务。
 * 跨服务规则：办卡前必须通过 HTTP 调用 member-service 确认会员存在；
 * member-service 不可用时返回明确错误，绝不跳过校验写入本地数据。
 */
@Service
public class MembershipCardService {

	private final MembershipCardRepository cardRepository;
	private final MemberClient memberClient;

	public MembershipCardService(MembershipCardRepository cardRepository, MemberClient memberClient) {
		this.cardRepository = cardRepository;
		this.memberClient = memberClient;
	}

	/**
	 * 办卡。
	 * 业务规则 1（跨服务）：会员必须存在于 member-service（不存在 → 404，服务不可用 → 503）。
	 * 业务规则 2：会员卡有效期必须合法，结束日期必须晚于开始日期。
	 * 业务规则 3：次卡必须指定大于 0 的剩余次数。
	 */
	@Transactional
	public MembershipCard createCard(CreateCardRequest request) {
		// 跨服务校验：RemoteServiceException/NotFoundException 会直接中止流程
		MemberDto member = memberClient.getMember(request.getMemberId());

		if (!request.getEndDate().isAfter(request.getStartDate())) {
			throw new InvalidRequestException("会员卡结束日期必须晚于开始日期");
		}
		if (request.getCardType() == CardType.TIMES
				&& (request.getRemainingTimes() == null || request.getRemainingTimes() <= 0)) {
			throw new InvalidRequestException("次卡必须指定大于 0 的剩余次数");
		}

		MembershipCard card = new MembershipCard();
		card.setMemberId(member.getId());
		card.setCardType(request.getCardType());
		card.setStartDate(request.getStartDate());
		card.setEndDate(request.getEndDate());
		card.setRemainingTimes(request.getRemainingTimes());
		return cardRepository.save(card);
	}

	/** 按 ID 查询会员卡，不存在时返回 404（checkin-service 验卡也依赖此接口） */
	@Transactional(readOnly = true)
	public MembershipCard getCard(Long cardId) {
		return cardRepository.findById(cardId)
				.orElseThrow(() -> new NotFoundException("会员卡不存在: id=" + cardId));
	}

	/** 查询某会员名下的全部会员卡 */
	@Transactional(readOnly = true)
	public List<MembershipCard> listCards(Long memberId) {
		return cardRepository.findByMemberId(memberId);
	}
}
