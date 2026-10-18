package com.zjgsu.yz.gym;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import com.zjgsu.yz.gym.repository.MemberRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * API 集成测试：验证 REST 接口的正常响应与错误响应（404 / 409 / 400）。
 * 使用随机端口真实启动应用，通过 HTTP 调用验证。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GymApiIntegrationTest {

	@Autowired
	private Environment environment;

	@Autowired
	private MemberRepository memberRepository;

	private RestTemplate restTemplate;
	private String base;

	@BeforeEach
	void setUp() {
		Integer port = environment.getProperty("local.server.port", Integer.class);
		base = "http://localhost:" + port;
		restTemplate = new RestTemplate();
		// 关闭默认错误抛出行为：让 4xx/5xx 状态码以 ResponseEntity 返回，便于断言
		restTemplate.setErrorHandler(new ResponseErrorHandler() {
			@Override
			public boolean hasError(ClientHttpResponse response) {
				return false;
			}
		});
	}

	@AfterEach
	void cleanUp() {
		memberRepository.deleteAll();
	}

	private String uniquePhone() {
		return "136" + MemberRepositoryTest.uniqueSuffix();
	}

	private Map<String, Object> memberBody(String name, String phone) {
		Map<String, Object> body = new HashMap<>();
		body.put("name", name);
		body.put("phone", phone);
		body.put("gender", "女");
		body.put("birthday", "1997-08-08");
		return body;
	}

	private Map<String, Object> cardBody(String cardType, String start, String end, Integer times) {
		Map<String, Object> body = new HashMap<>();
		body.put("cardType", cardType);
		body.put("startDate", start);
		body.put("endDate", end);
		body.put("remainingTimes", times);
		return body;
	}

	@Test
	void createMemberThenQueryShouldWork() {
		String phone = uniquePhone();
		ResponseEntity<Map> created = restTemplate.postForEntity(base + "/api/members",
				memberBody("接口测试会员", phone), Map.class);
		assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(created.getBody().get("id")).isNotNull();

		Long id = ((Number) created.getBody().get("id")).longValue();

		ResponseEntity<Map> got = restTemplate.getForEntity(base + "/api/members/" + id, Map.class);
		assertThat(got.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(got.getBody().get("phone")).isEqualTo(phone);

		ResponseEntity<Map[]> all = restTemplate.getForEntity(base + "/api/members", Map[].class);
		assertThat(all.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(all.getBody()).anySatisfy(m -> assertThat(m.get("phone")).isEqualTo(phone));
	}

	@Test
	void duplicatePhoneShouldReturn409() {
		String phone = uniquePhone();
		restTemplate.postForEntity(base + "/api/members", memberBody("首次登记", phone), Map.class);

		ResponseEntity<Map> second = restTemplate.postForEntity(base + "/api/members",
				memberBody("重复登记", phone), Map.class);
		assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
	}

	@Test
	void cardForUnknownMemberShouldReturn404() {
		ResponseEntity<Map> resp = restTemplate.postForEntity(base + "/api/members/999999/cards",
				cardBody("MONTHLY", "2026-10-18", "2026-11-18", null), Map.class);
		assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void invalidCardPeriodShouldReturn400() {
		Long id = createMemberAndGetId();

		ResponseEntity<Map> bad = restTemplate.postForEntity(base + "/api/members/" + id + "/cards",
				cardBody("MONTHLY", "2026-10-18", "2026-10-01", null), Map.class);
		assertThat(bad.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
	}

	@Test
	void timesCardWithoutTimesShouldReturn400AndValidCardCreated() {
		Long id = createMemberAndGetId();

		ResponseEntity<Map> noTimes = restTemplate.postForEntity(base + "/api/members/" + id + "/cards",
				cardBody("TIMES", "2026-10-18", "2027-10-18", null), Map.class);
		assertThat(noTimes.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

		ResponseEntity<Map> created = restTemplate.postForEntity(base + "/api/members/" + id + "/cards",
				cardBody("TIMES", "2026-10-18", "2027-10-18", 30), Map.class);
		assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(created.getBody().get("status")).isEqualTo("ACTIVE");

		ResponseEntity<Map[]> cards = restTemplate.getForEntity(base + "/api/members/" + id + "/cards", Map[].class);
		assertThat(cards.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(cards.getBody()).hasSize(1);
	}

	private Long createMemberAndGetId() {
		ResponseEntity<Map> created = restTemplate.postForEntity(base + "/api/members",
				memberBody("办卡流程会员", uniquePhone()), Map.class);
		return ((Number) created.getBody().get("id")).longValue();
	}
}
