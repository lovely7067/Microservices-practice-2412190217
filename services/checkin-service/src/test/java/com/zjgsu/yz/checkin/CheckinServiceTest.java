package com.zjgsu.yz.checkin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zjgsu.yz.checkin.client.CardClient;
import com.zjgsu.yz.checkin.exception.InvalidRequestException;
import com.zjgsu.yz.checkin.exception.NotFoundException;
import com.zjgsu.yz.checkin.exception.RemoteServiceException;
import com.zjgsu.yz.checkin.model.CardStatus;
import com.zjgsu.yz.checkin.model.CheckinRecord;
import com.zjgsu.yz.checkin.repository.CheckinRecordRepository;
import com.zjgsu.yz.checkin.service.CheckinService;
import com.zjgsu.yz.checkin.web.dto.CardDto;
import com.zjgsu.yz.checkin.web.dto.CreateCheckinRequest;

/**
 * 入场核验服务业务规则测试（Mockito 模拟跨服务调用，不依赖 card-service 运行）。
 */
@ExtendWith(MockitoExtension.class)
class CheckinServiceTest {

	@Mock
	private CheckinRecordRepository checkinRepository;

	@Mock
	private CardClient cardClient;

	private CheckinService checkinService;

	@BeforeEach
	void setUp() {
		checkinService = new CheckinService(checkinRepository, cardClient);
	}

	private CreateCheckinRequest request(Long memberId, Long cardId) {
		CreateCheckinRequest req = new CreateCheckinRequest();
		req.setMemberId(memberId);
		req.setCardId(cardId);
		return req;
	}

	private CardDto card(Long id, Long memberId, CardStatus status, LocalDate endDate) {
		CardDto dto = new CardDto();
		dto.setId(id);
		dto.setMemberId(memberId);
		dto.setCardType("TIMES");
		dto.setStatus(status);
		dto.setEndDate(endDate);
		dto.setRemainingTimes(30);
		return dto;
	}

	@Test
	void 卡片有效且属于该会员时核验成功() {
		when(cardClient.getCard(1L)).thenReturn(card(1L, 2L, CardStatus.ACTIVE, LocalDate.now().plusYears(1)));
		when(checkinRepository.save(any(CheckinRecord.class)))
				.thenAnswer(inv -> inv.getArgument(0));

		CheckinRecord record = checkinService.checkin(request(2L, 1L));

		assertEquals(2L, record.getMemberId());
		assertEquals(1L, record.getCardId());
		assertEquals(true, record.getAllowed());
	}

	@Test
	void 卡片不存在时返回404且不写库() {
		when(cardClient.getCard(99L)).thenThrow(new NotFoundException("会员卡不存在: id=99"));
		assertThrows(NotFoundException.class, () -> checkinService.checkin(request(2L, 99L)));
	}

	@Test
	void 卡片服务不可用时返回503且不写库() {
		when(cardClient.getCard(1L)).thenThrow(new RemoteServiceException("card-service 不可用"));
		assertThrows(RemoteServiceException.class, () -> checkinService.checkin(request(2L, 1L)));
	}

	@Test
	void 卡片不属于该会员时拒绝() {
		when(cardClient.getCard(1L)).thenReturn(card(1L, 3L, CardStatus.ACTIVE, LocalDate.now().plusYears(1)));
		assertThrows(InvalidRequestException.class, () -> checkinService.checkin(request(2L, 1L)));
	}

	@Test
	void 卡片状态非正常时拒绝() {
		when(cardClient.getCard(1L)).thenReturn(card(1L, 2L, CardStatus.FROZEN, LocalDate.now().plusYears(1)));
		assertThrows(InvalidRequestException.class, () -> checkinService.checkin(request(2L, 1L)));
	}

	@Test
	void 卡片已过期时拒绝() {
		when(cardClient.getCard(1L)).thenReturn(card(1L, 2L, CardStatus.ACTIVE, LocalDate.now().minusDays(1)));
		assertThrows(InvalidRequestException.class, () -> checkinService.checkin(request(2L, 1L)));
	}
}
