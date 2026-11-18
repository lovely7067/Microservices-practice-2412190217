package com.zjgsu.yz.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * 第 09 周认证链路测试（H2 内存库，不依赖真实 MySQL/Nacos）：
 * 登录成功 / 密码错误 / 用户不存在 / 管理接口防绕过（缺少网关签名头 → 403）。
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowTest {

	private static final String GATEWAY_SECRET = "test-only-jwt-secret-for-unit-tests-32bytes";

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("正确账号密码登录 → 200 且返回 Token 与 USER 角色")
	void loginSuccess() throws Exception {
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"username\":\"user1\",\"password\":\"user123\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").isNotEmpty())
				.andExpect(jsonPath("$.role").value("USER"))
				.andExpect(jsonPath("$.tokenType").value("Bearer"));
	}

	@Test
	@DisplayName("密码错误 → 401")
	void loginWrongPassword() throws Exception {
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"username\":\"user1\",\"password\":\"wrong\"}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("不存在的用户名 → 401（不区分用户名/密码错误）")
	void loginUnknownUser() throws Exception {
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"username\":\"nobody\",\"password\":\"whatever\"}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("管理接口：缺少网关签名头（绕过网关直连）→ 403")
	void adminEndpointRejectsDirectAccess() throws Exception {
		mockMvc.perform(get("/api/members/statistics"))
				.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("管理接口：携带正确网关签名头 → 200 且包含统计字段")
	void adminEndpointAcceptsGatewayHeader() throws Exception {
		MvcResult result = mockMvc.perform(get("/api/members/statistics")
						.header("X-Gateway-Auth", GATEWAY_SECRET))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalMembers").exists())
				.andExpect(jsonPath("$.maleCount").exists())
				.andExpect(jsonPath("$.femaleCount").exists())
				.andReturn();
		assertThat(result.getResponse().getContentAsString()).contains("totalMembers");
	}
}