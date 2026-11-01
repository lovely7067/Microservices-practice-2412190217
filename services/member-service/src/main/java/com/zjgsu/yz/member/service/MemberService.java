package com.zjgsu.yz.member.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zjgsu.yz.member.exception.DuplicatePhoneException;
import com.zjgsu.yz.member.exception.NotFoundException;
import com.zjgsu.yz.member.model.Member;
import com.zjgsu.yz.member.repository.MemberRepository;
import com.zjgsu.yz.member.web.dto.CreateMemberRequest;

/**
 * 会员业务服务：承载会员登记与查询的业务规则。
 */
@Service
public class MemberService {

	private final MemberRepository memberRepository;

	public MemberService(MemberRepository memberRepository) {
		this.memberRepository = memberRepository;
	}

	/**
	 * 会员登记。
	 * 业务规则：同一手机号只能注册一个会员，重复登记返回 409。
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

	/** 按 ID 查询会员，不存在时返回 404（card-service 办卡校验也依赖此接口） */
	@Transactional(readOnly = true)
	public Member getMember(Long id) {
		return memberRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("会员不存在: id=" + id));
	}
}
