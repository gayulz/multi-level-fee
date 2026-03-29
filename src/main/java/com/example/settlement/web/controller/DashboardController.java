package com.example.settlement.web.controller;

import com.example.settlement.domain.entity.User;
import com.example.settlement.dto.response.DashboardResponse;
import com.example.settlement.service.DashboardService;
import com.example.settlement.web.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * [NEW] 대시보드 컨트롤러
 *
 * 권한별 대시보드 화면을 제공한다.
 *
 * @author gayul.kim
 * @since 2025-01-30
 */
@Controller
@RequiredArgsConstructor
public class DashboardController {

	private final DashboardService dashboardService;

	/**
	 * 대시보드 메인 화면
	 *
	 * @param userDetails 현재 로그인한 사용자
	 * @param model 뷰 모델
	 * @return 대시보드 템플릿
	 */
	@GetMapping("/dashboard")
	public String dashboard(
		@AuthenticationPrincipal CustomUserDetails userDetails,
		Model model
	) {
		User user = userDetails.getUser();

		// 대시보드 데이터 조회
		DashboardResponse dashboardData = dashboardService.getDashboardData(user);

		model.addAttribute("pageTitle", "대시보드");
		model.addAttribute("statistics", dashboardData.getStatistics());
		model.addAttribute("recentSettlements", dashboardData.getRecentSettlements());
		model.addAttribute("organizationTree", dashboardData.getOrganizationTree());
		model.addAttribute("user", user);

		return "pages/dashboard";
	}
}
