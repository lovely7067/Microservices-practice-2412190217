package com.zjgsu.yz.gym.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zjgsu.yz.gym.exception.DuplicatePhoneException;
import com.zjgsu.yz.gym.exception.InvalidRequestException;
import com.zjgsu.yz.gym.exception.NotFoundException;
import com.zjgsu.yz.gym.model.CardType;
import com.zjgsu.yz.gym.model.Member;
import com.zjgsu.yz.gym.model.MembershipCard;
import com.zjgsu.yz.gym.repository.MemberRepository;
import com.zjgsu.yz.gym.repository.MembershipCardRepository;
import com.zjgsu.yz.gym.web.dto.CreateCardRequest;
import com.zjgsu.yz.gym.web.dto.CreateMemberRequest;

/**
 * 会员业务服务：承载核心业务规则，Controller 不直接编写数据访问逻辑。
 */
@Service
public class MemberService {

	private final MemberRepository memberRepository;
	private final MembershipCardRepository cardRepository;

	public MemberService(MemberRepository memberRepository, MembershipCardRepository cardRepository) {
		this.memberRepository = memberRepository;
		this.cardRepository = cardRepository;
	}

	/**
	 * 会员登记。
	 * 业务规则 1：同一手机号只能注册一个会员，重复登记返回 409。
	 */
	@Transactional
	public Member createMember(CreateMemberRequest request) {
		if (memberRepository.existsByPhone(request.getPhone())) {
			throw new DuplicatePhoneException(request.getPhone());
		}
		Member member = new Member();
		member.setName(request.getName());
		member.setPhone(request.getPhone());
		member.setGender(request.getGender());
		member.setBirthday(request.getBirthday());
		return memberRepository.save(member);
	}

	/** 查询全部会员 */
	@Transactional(readOnly = true)
	public List<Member> listMembers() {
		return memberRepository.findAll();
	}

	/** 按ID查询会员，不存在时返回 404 */
	@Transactional(readOnly = true)
	public Member getMember(Long id) {
		return memberRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("会员不存在: id=" + id));
	}

	/**
	 * 为会员办卡。
	 * 业务规则 2：会员卡有效期必须合法，结束日期必须晚于开始日期。
	 * 业务规则 3：次卡必须指定大于 0 的剩余次数。
	 */
	@Transactional
	public MembershipCard createCard(Long memberId, CreateCardRequest request) {
		Member member = getMember(memberId);
		if (!request.getEndDate().isAfter(request.getStartDate())) {
			throw new InvalidRequestException("会员卡结束日期必须晚于开始日期");
		}
		if (request.getCardType() == CardType.TIMES
				&& (request.getRemainingTimes() == null || request.getRemainingTimes() <= 0)) {
			throw new InvalidRequestException("次卡必须指定大于 0 的剩余次数");
		}
		MembershipCard card = new MembershipCard();
		card.setCardType(request.getCardType());
		card.setStartDate(request.getStartDate());
		card.setEndDate(request.getEndDate());
		card.setRemainingTimes(request.getRemainingTimes());
		card.setMember(member);
		member.getCards().add(card);
		return cardRepository.save(card);
	}

	/** 查询某会员名下的全部会员卡 */
	@Transactional(readOnly = true)
	public List<MembershipCard> listCards(Long memberId) {
		getMember(memberId);
		return cardRepository.findByMemberId(memberId);
	}
}
