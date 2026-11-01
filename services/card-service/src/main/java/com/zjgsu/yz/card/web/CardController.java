package com.zjgsu.yz.card.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.zjgsu.yz.card.model.MembershipCard;
import com.zjgsu.yz.card.service.MembershipCardService;
import com.zjgsu.yz.card.web.dto.CardResponse;
import com.zjgsu.yz.card.web.dto.CreateCardRequest;

import jakarta.validation.Valid;

/**
 * 会员卡 REST 接口：只负责 HTTP 输入输出，业务逻辑与数据访问均委托给 Service。
 */
@RestController
@RequestMapping("/api/cards")
@Validated
public class CardController {

	private final MembershipCardService cardService;

	public CardController(MembershipCardService cardService) {
		this.cardService = cardService;
	}

	/** 办卡（创建会员卡；内部会调用 member-service 校验会员存在） */
	@PostMapping
	public ResponseEntity<CardResponse> createCard(@Valid @RequestBody CreateCardRequest request) {
		MembershipCard card = cardService.createCard(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(CardResponse.of(card));
	}

	/** 按 ID 查询单张会员卡（checkin-service 入场验卡也调用此接口） */
	@GetMapping("/{id}")
	public ResponseEntity<CardResponse> getCard(@PathVariable Long id) {
		return ResponseEntity.ok(CardResponse.of(cardService.getCard(id)));
	}

	/** 查询某会员名下的全部会员卡 */
	@GetMapping
	public ResponseEntity<List<CardResponse>> listCards(@RequestParam Long memberId) {
		List<CardResponse> list = cardService.listCards(memberId).stream()
				.map(CardResponse::of)
				.toList();
		return ResponseEntity.ok(list);
	}
}
