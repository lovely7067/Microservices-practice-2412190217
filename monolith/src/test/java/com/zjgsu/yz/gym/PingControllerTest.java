package com.zjgsu.yz.gym;

import com.zjgsu.yz.gym.web.PingController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 验证 GET /api/ping 接口可正常访问并返回运行状态。
 */
class PingControllerTest {

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(new PingController()).build();
	}

	@Test
	void pingShouldReturnUp() throws Exception {
		String body = mockMvc.perform(get("/api/ping"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		assertThat(body).contains("\"status\":\"UP\"", "\"app\":\"gym\"");
	}
}
