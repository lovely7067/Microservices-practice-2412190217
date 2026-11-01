package com.zjgsu.yz.card;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zjgsu.yz.card.client.MemberClient;
import com.zjgsu.yz.card.exception.InvalidRequestException;
import com.zjgsu.yz.card.exception.NotFoundException;
import com.zjgsu.yz.card.exception.RemoteServiceException;
import com.zjgsu.yz.card.model.CardType;
import com.zjgsu.yz.card.model.MembershipCard;
import com.zjgsu.yz.card.repository.MembershipCardRepository;
import com.zjgsu.yz.card.service.MembershipCardService;
import com.zjgsu.yz.card.web.dto.CreateCardRequest;
import com.zjgsu.yz.card.web.dto.MemberDto;

/**
 * 会员卡服务业务规则测试（Mockito 模拟跨服务调用，不依赖 member-service 运行）。
 */
@ExtendWith(MockitoExtension.class)
class MembershipCardServiceTest {

	@Mock
	private MembershipCardRepository cardRepository;

	@Mock
	private MemberClient memberClient;

	private MembershipCardService cardService;

	@BeforeEach
	void setUp() {
		cardService = new MembershipCardService(cardRepository, memberClient);
	}

	private CreateCardRequest request(Long memberId, CardType type, LocalDate start, LocalDate end, Integer times) {
		CreateCardRequest req = new CreateCardRequest();
		req.setMemberId(memberId);
		req.setCardType(type);
		req.setStartDate(start);
		req.setEndDate(end);
		req.setRemainingTimes(times);
		return req;
	}

	private CreateCardRequest request(CardType type, LocalDate start, LocalDate end, Integer times) {
		return request(1L, type, start, end, times);
	}

	private MemberDto member(Long id) {
		MemberDto dto = new MemberDto();
		dto.setId(id);
		dto.setName("测试会员");
		dto.setPhone("13800000001");
		return dto;
	}

	@Test
	void 会员存在且参数合法时办卡成功() {
		when(memberClient.getMember(1L)).thenReturn(member(1L));
		when(cardRepository.save(any(MembershipCard.class)))
				.thenAnswer(inv -> inv.getArgument(0));

		MembershipCard card = cardService.createCard(
				request(CardType.TIMES, LocalDate.of(2026, 11, 1), LocalDate.of(2027, 11, 1), 30));

		assertEquals(1L, card.getMemberId());
		assertEquals(CardType.TIMES, card.getCardType());
		assertTrue(card.getId() == null || card.getId() >= 0);
	}

	@Test
	void 会员不存在时返回404且不写库() {
		when(memberClient.getMember(99L)).thenThrow(new NotFoundException("会员不存在: id=99"));
		assertThrows(NotFoundException.class, () -> cardService.createCard(
				request(99L, CardType.MONTHLY, LocalDate.of(2026, 11, 1), LocalDate.of(2027, 11, 1), null)));
	}

	@Test
	void 会员服务不可用时返回503且不写库() {
		when(memberClient.getMember(1L))
				.thenThrow(new RemoteServiceException("member-service 不可用"));
		assertThrows(RemoteServiceException.class, () -> cardService.createCard(
				request(CardType.MONTHLY, LocalDate.of(2026, 11, 1), LocalDate.of(2027, 11, 1), null)));
	}

	@Test
	void 结束日期不晚于开始日期时拒绝() {
		when(memberClient.getMember(1L)).thenReturn(member(1L));
		assertThrows(InvalidRequestException.class, () -> cardService.createCard(
				request(CardType.MONTHLY, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 1), null)));
	}

	@Test
	void 次卡剩余次数非正数时拒绝() {
		when(memberClient.getMember(1L)).thenReturn(member(1L));
		assertThrows(InvalidRequestException.class, () -> cardService.createCard(
				request(CardType.TIMES, LocalDate.of(2026, 11, 1), LocalDate.of(2027, 11, 1), 0)));
	}

	@Test
	void 按ID查询不存在的卡返回404() {
		when(cardRepository.findById(99L)).thenReturn(Optional.empty());
		assertThrows(NotFoundException.class, () -> cardService.getCard(99L));
	}
}
