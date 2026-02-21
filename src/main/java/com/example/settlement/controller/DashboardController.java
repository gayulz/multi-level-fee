package com.example.settlement.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * [NEW] 대시보드 Controller
 *
 * 대시보드 페이지를 렌더링하고 Mock 데이터를 제공합니다.
 * 시스템 전체의 핵심 지표(노드 수, 정산 건수, 금액, 처리 시간)를 표시합니다.
 *
 * @author gayul.kim
 * @since 2026-02-21
 */
@Controller
public class DashboardController {

	/**
	 * 대시보드 메인 페이지
	 *
	 * @param model Model 객체
	 * @return 대시보드 페이지 뷰 이름
	 */
	@GetMapping("/")
	public String dashboard(Model model) {
		// 요약 카드 데이터
		model.addAttribute("totalNodes", 15);
		model.addAttribute("todaySettlements", 42);
		model.addAttribute("totalAmount", 12_500_000L);
		model.addAttribute("avgProcessTime", 235);

		// 최근 정산 내역 (5건)
		model.addAttribute("recentSettlements", createMockSettlements());

		// 페이지 식별자 (사이드바 활성화용)
		model.addAttribute("currentPage", "dashboard");
		model.addAttribute("pageTitle", "대시보드");

		return "pages/dashboard";
	}

	/**
	 * Mock 정산 내역 데이터 생성
	 *
	 * @return 최근 5건의 정산 내역
	 */
	private List<Map<String, Object>> createMockSettlements() {
		List<Map<String, Object>> list = new ArrayList<>();

		for (int i = 1; i <= 5; i++) {
			list.add(Map.of(
				"id", (long) i,
				"orderId", String.format("ORD-20260221-%03d", i),
				"amount", 10_000L * i,
				"totalFee", (long) (10_000L * i * 0.1705),
				"status", i <= 3 ? "COMPLETED" : (i == 4 ? "PENDING" : "FAILED"),
				"createdAt", LocalDateTime.now().minusHours(i)
			));
		}

		return list;
	}
}
