package com.zjgsu.yz.gym;

import java.time.LocalDate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.zjgsu.yz.gym.exception.DuplicatePhoneException;
import com.zjgsu.yz.gym.exception.InvalidRequestException;
import com.zjgsu.yz.gym.model.CardStatus;
import com.zjgsu.yz.gym.model.CardType;
import com.zjgsu.yz.gym.model.Member;
import com.zjgsu.yz.gym.model.MembershipCard;
import com.zjgsu.yz.gym.repository.MemberRepository;
import com.zjgsu.yz.gym.service.MemberService;
import com.zjgsu.yz.gym.web.dto.CreateCardRequest;
import com.zjgsu.yz.gym.web.dto.CreateMemberRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 验证 Service 层业务规则（手机号唯一、卡有效期校验、次卡次数校验）。
 */
@SpringBootTest
class MemberServiceTest {

	@Autowired
	private MemberService memberService;

	@Autowired
	private MemberRepository memberRepository;

	@AfterEach
	void cleanUp() {
		memberRepository.deleteAll();
	}

	private CreateMemberRequest memberRequest(String name, String phone) {
		CreateMemberRequest request = new CreateMemberRequest();
		request.setName(name);
		request.setPhone(phone);
		request.setGender("男");
		request.setBirthday(LocalDate.of(1996, 5, 20));
		return request;
	}

	@Test
	void duplicatePhoneShouldBeRejected() {
		String phone = "139" + MemberRepositoryTest.uniqueSuffix();
		memberService.createMember(memberRequest("第一个会员", phone));

		assertThatThrownBy(() -> memberService.createMember(memberRequest("重复会员", phone)))
				.isInstanceOf(DuplicatePhoneException.class);
	}

	@Test
	void cardEndDateMustBeAfterStartDate() {
		Member member = memberService.createMember(memberRequest("有效期测试会员", "139" + MemberRepositoryTest.uniqueSuffix()));

		CreateCardRequest bad = new CreateCardRequest();
		bad.setCardType(CardType.MONTHLY);
		bad.setStartDate(LocalDate.of(2026, 10, 18));
		bad.setEndDate(LocalDate.of(2026, 10, 18));
		assertThatThrownBy(() -> memberService.createCard(member.getId(), bad))
				.isInstanceOf(InvalidRequestException.class);
	}

	@Test
	void timesCardRequiresPositiveRemainingTimes() {
		Member member = memberService.createMember(memberRequest("次卡测试会员", "139" + MemberRepositoryTest.uniqueSuffix()));

		CreateCardRequest bad = new CreateCardRequest();
		bad.setCardType(CardType.TIMES);
		bad.setStartDate(LocalDate.of(2026, 10, 18));
		bad.setEndDate(LocalDate.of(2027, 10, 18));
		bad.setRemainingTimes(0);
		assertThatThrownBy(() -> memberService.createCard(member.getId(), bad))
				.isInstanceOf(InvalidRequestException.class);
	}

	@Test
	void validCardShouldBeCreatedWithActiveStatus() {
		Member member = memberService.createMember(memberRequest("正常办卡会员", "139" + MemberRepositoryTest.uniqueSuffix()));

		CreateCardRequest good = new CreateCardRequest();
		good.setCardType(CardType.TIMES);
		good.setStartDate(LocalDate.of(2026, 10, 18));
		good.setEndDate(LocalDate.of(2027, 10, 18));
		good.setRemainingTimes(30);

		MembershipCard card = memberService.createCard(member.getId(), good);
		assertThat(card.getId()).isNotNull();
		assertThat(card.getStatus()).isEqualTo(CardStatus.ACTIVE);
		assertThat(card.getRemainingTimes()).isEqualTo(30);
	}
}
