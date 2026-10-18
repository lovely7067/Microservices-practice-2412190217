package com.zjgsu.yz.gym;

import java.time.LocalDate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.zjgsu.yz.gym.model.CardType;
import com.zjgsu.yz.gym.model.Member;
import com.zjgsu.yz.gym.model.MembershipCard;
import com.zjgsu.yz.gym.repository.MemberRepository;
import com.zjgsu.yz.gym.repository.MembershipCardRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 Repository 层可正常读写（基于 H2 内存库，可重复运行）。
 */
@SpringBootTest
class MemberRepositoryTest {

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private MembershipCardRepository cardRepository;

	@AfterEach
	void cleanUp() {
		memberRepository.deleteAll();
	}

	@Test
	void saveAndFindMember() {
		Member member = new Member();
		member.setName("仓库测试会员");
		member.setPhone("139" + uniqueSuffix());
		member.setGender("男");
		member.setBirthday(LocalDate.of(1990, 1, 1));

		Member saved = memberRepository.save(member);
		assertThat(saved.getId()).isNotNull();

		Member found = memberRepository.findById(saved.getId()).orElseThrow();
		assertThat(found.getName()).isEqualTo("仓库测试会员");
		assertThat(found.getPhone()).isEqualTo(saved.getPhone());
		assertThat(found.getCreatedAt()).isNotNull();

		assertThat(memberRepository.existsByPhone(saved.getPhone())).isTrue();
		assertThat(memberRepository.existsByPhone("199" + uniqueSuffix())).isFalse();
	}

	@Test
	void saveCardAndFindByMemberId() {
		Member member = new Member();
		member.setName("持卡测试会员");
		member.setPhone("139" + uniqueSuffix());
		member = memberRepository.save(member);

		MembershipCard card = new MembershipCard();
		card.setCardType(CardType.TIMES);
		card.setStartDate(LocalDate.of(2026, 10, 18));
		card.setEndDate(LocalDate.of(2027, 10, 18));
		card.setRemainingTimes(30);
		card.setMember(member);
		member.getCards().add(card);
		cardRepository.save(card);

		assertThat(cardRepository.findByMemberId(member.getId())).hasSize(1);
		assertThat(cardRepository.findAll()).isNotEmpty();
	}

	static String uniqueSuffix() {
		String nano = String.valueOf(System.nanoTime());
		return nano.substring(nano.length() - 8);
	}
}
