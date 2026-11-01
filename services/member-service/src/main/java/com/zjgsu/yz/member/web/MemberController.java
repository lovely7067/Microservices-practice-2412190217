package com.zjgsu.yz.member.web;

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

import com.zjgsu.yz.member.model.Member;
import com.zjgsu.yz.member.service.MemberService;
import com.zjgsu.yz.member.web.dto.CreateMemberRequest;
import com.zjgsu.yz.member.web.dto.MemberResponse;

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

	/** 按 ID 查询单个会员（card-service 办卡前的校验也调用此接口） */
	@GetMapping("/{id}")
	public ResponseEntity<MemberResponse> getMember(@PathVariable Long id) {
		return ResponseEntity.ok(MemberResponse.of(memberService.getMember(id)));
	}
}
