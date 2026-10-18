package com.zjgsu.yz.gym.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zjgsu.yz.gym.model.Member;
import com.zjgsu.yz.gym.model.MembershipCard;
import com.zjgsu.yz.gym.service.MemberService;
import com.zjgsu.yz.gym.web.dto.CardResponse;
import com.zjgsu.yz.gym.web.dto.CreateCardRequest;
import com.zjgsu.yz.gym.web.dto.CreateMemberRequest;
import com.zjgsu.yz.gym.web.dto.MemberResponse;

import jakarta.validation.Valid;

/**
 * 会员 REST 接口：只负责 HTTP 输入输出，业务逻辑与数据访问均委托给 Service。
 */
@RestController
@RequestMapping("/api/members")
@Validated
public class MemberController {

	private final MemberService memberService;

	public MemberController(MemberService memberService) {
		this.memberService = memberService;
	}

	/** 会员登记（创建） */
	@PostMapping
	public ResponseEntity<MemberResponse> createMember(@Valid @RequestBody CreateMemberRequest request) {
		Member member = memberService.createMember(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(MemberResponse.of(member));
	}

	/** 查询全部会员 */
	@GetMapping
	public ResponseEntity<List<MemberResponse>> listMembers() {
		List<MemberResponse> list = memberService.listMembers().stream()
				.map(MemberResponse::of)
				.toList();
		return ResponseEntity.ok(list);
	}

	/** 按 ID 查询单个会员 */
	@GetMapping("/{id}")
	public ResponseEntity<MemberResponse> getMember(@PathVariable Long id) {
		return ResponseEntity.ok(MemberResponse.of(memberService.getMember(id)));
	}

	/** 为会员办卡（创建会员卡） */
	@PostMapping("/{id}/cards")
	public ResponseEntity<CardResponse> createCard(@PathVariable Long id,
			@Valid @RequestBody CreateCardRequest request) {
		MembershipCard card = memberService.createCard(id, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(CardResponse.of(card));
	}

	/** 查询某会员名下的会员卡 */
	@GetMapping("/{id}/cards")
	public ResponseEntity<List<CardResponse>> listCards(@PathVariable Long id) {
		List<CardResponse> list = memberService.listCards(id).stream()
				.map(CardResponse::of)
				.toList();
		return ResponseEntity.ok(list);
	}
}
