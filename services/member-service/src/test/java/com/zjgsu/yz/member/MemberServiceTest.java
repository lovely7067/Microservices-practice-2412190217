package com.zjgsu.yz.member;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.zjgsu.yz.member.exception.DuplicatePhoneException;
import com.zjgsu.yz.member.exception.NotFoundException;
import com.zjgsu.yz.member.model.Member;
import com.zjgsu.yz.member.service.MemberService;
import com.zjgsu.yz.member.web.dto.CreateMemberRequest;

/**
 * 会员服务业务规则测试（H2 内存库）。
 */
@SpringBootTest
class MemberServiceTest {

	@Autowired
	private MemberService memberService;

	private CreateMemberRequest request(String name, String phone) {
		CreateMemberRequest req = new CreateMemberRequest();
		req.setName(name);
		req.setPhone(phone);
		req.setGender("男");
		req.setBirthday(java.time.LocalDate.of(1995, 6, 1));
		return req;
	}

	@Test
	void 登记会员成功() {
		Member member = memberService.createMember(request("测试会员A", "13800000001"));
		assertTrue(member.getId() > 0);
		assertEquals("测试会员A", member.getName());
	}

	@Test
	void 重复手机号登记被拒绝() {
		memberService.createMember(request("测试会员B", "13800000002"));
		assertThrows(DuplicatePhoneException.class,
				() -> memberService.createMember(request("测试会员B2", "13800000002")));
	}

	@Test
	void 查询不存在的会员返回异常() {
		assertThrows(NotFoundException.class, () -> memberService.getMember(99999L));
	}
}
