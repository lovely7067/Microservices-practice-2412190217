package com.zjgsu.yz.checkin.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.zjgsu.yz.checkin.model.CheckinRecord;
import com.zjgsu.yz.checkin.service.CheckinService;
import com.zjgsu.yz.checkin.web.dto.CheckinResponse;
import com.zjgsu.yz.checkin.web.dto.CreateCheckinRequest;

import jakarta.validation.Valid;

/**
 * 入场核验 REST 接口：只负责 HTTP 输入输出，业务逻辑与数据访问均委托给 Service。
 */
@RestController
@RequestMapping("/api/checkins")
@Validated
public class CheckinController {

	private final CheckinService checkinService;

	public CheckinController(CheckinService checkinService) {
		this.checkinService = checkinService;
	}

	/** 入场核验（内部会调用 card-service 验卡） */
	@PostMapping
	public ResponseEntity<CheckinResponse> checkin(@Valid @RequestBody CreateCheckinRequest request) {
		CheckinRecord record = checkinService.checkin(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(CheckinResponse.of(record));
	}

	/** 查询全部入场记录 */
	@GetMapping
	public ResponseEntity<List<CheckinResponse>> listCheckins(@RequestParam(required = false) Long memberId) {
		List<CheckinRecord> records = (memberId != null)
				? checkinService.listByMember(memberId)
				: checkinService.listCheckins();
		List<CheckinResponse> list = records.stream()
				.map(CheckinResponse::of)
				.toList();
		return ResponseEntity.ok(list);
	}
}
