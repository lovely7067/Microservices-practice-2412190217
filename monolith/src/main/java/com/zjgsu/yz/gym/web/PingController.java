package com.zjgsu.yz.gym.web;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 服务运行状态验证接口。
 * 启动应用后访问 GET http://localhost:8080/api/ping 可确认服务是否正常运行。
 */
@RestController
@RequestMapping("/api")
public class PingController {

	@GetMapping("/ping")
	public Map<String, Object> ping() {
		return Map.of(
				"status", "UP",
				"app", "gym",
				"time", LocalDateTime.now().toString()
		);
	}
}
