package com.example.settlement.controller;

import com.example.settlement.domain.entity.User;
import com.example.settlement.domain.entity.enums.UserRole;
import com.example.settlement.service.ApprovalService;
import com.example.settlement.service.SettlementService;
import com.example.settlement.service.UserService;
import com.example.settlement.web.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;

/**
 * [NEW] 권한별 대시보드 컨트롤러.
 *
 * SUPER_ADMIN: 전체 조직 통계 (실제 DB 데이터),
 * ROLE_ADMIN: 소속 조직 승인 대기 목록,
 * ROLE_USER: 본인 정산 내역.
 *
 * @author gayul.kim
 * @since 2026-03-09
 */
@Controller
@RequiredArgsConstructor
public class DashboardController {

	private final SettlementService settlementService;
	private final UserService userService;
	private final ApprovalService approvalService;

	/**
	 * [NEW] 대시보드 페이지 렌더링.
	 *
	 * 로그인된 사용자의 권한에 따라 서로 다른 통계 데이터를 대시보드에 표시합니다.
	 *
	 * @author gayul.kim
	 * @param userDetails 인증된 사용자 정보
	 * @param model       뷰 모델
	 * @return 대시보드 뷰 경로
	 */
	@GetMapping("/dashboard")
	public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
		User user = userDetails.getUser();

		// 공통 페이지 속성
		model.addAttribute("currentPage", "dashboard");
		model.addAttribute("pageTitle", "대시보드");

		if (user.hasRole(UserRole.ROLE_SUPER_ADMIN)) {
			// SUPER_ADMIN: 전체 조직 통계
			long totalRequests = settlementService.getTotalRequests();
			long activeUsers = userService.getActiveUsersCount();
			var recentSettlements = settlementService.getRecentRequests(10);
			BigDecimal totalAmount = recentSettlements.stream()
					.map(r -> r.getAmount() != null ? r.getAmount() : BigDecimal.ZERO)
					.reduce(BigDecimal.ZERO, BigDecimal::add);

			model.addAttribute("totalAmount", totalAmount.longValue());
			model.addAttribute("totalNodes", settlementService.getRootNodes().size());
			model.addAttribute("todaySettlements", totalRequests);
			model.addAttribute("avgProcessTime", activeUsers);
			model.addAttribute("recentSettlements", recentSettlements);

		} else if (user.hasRole(UserRole.ROLE_ADMIN)) {
			// ADMIN: 소속 조직 정산 내역
			var orgRequests = settlementService.getRequestsByOrganization(user.getOrganization().getOrgId());
			var pendingApprovals = approvalService.getPendingRequestsForApprover(user);
			BigDecimal totalAmount = orgRequests.stream()
					.map(r -> r.getAmount() != null ? r.getAmount() : BigDecimal.ZERO)
					.reduce(BigDecimal.ZERO, BigDecimal::add);

			model.addAttribute("totalAmount", totalAmount.longValue());
			model.addAttribute("totalNodes", orgRequests.size());
			model.addAttribute("todaySettlements", orgRequests.size());
			model.addAttribute("avgProcessTime", pendingApprovals.size());
			model.addAttribute("recentSettlements", orgRequests.stream().limit(10).toList());

		} else {
			// USER: 본인 정산 내역
			var myRequests = settlementService.getRequestsByUser(user);
			BigDecimal totalAmount = myRequests.stream()
					.map(r -> r.getAmount() != null ? r.getAmount() : BigDecimal.ZERO)
					.reduce(BigDecimal.ZERO, BigDecimal::add);

			model.addAttribute("totalAmount", totalAmount.longValue());
			model.addAttribute("totalNodes", 0);
			model.addAttribute("todaySettlements", myRequests.size());
			model.addAttribute("avgProcessTime", 0);
			model.addAttribute("recentSettlements", myRequests.stream().limit(10).toList());
		}

		return "pages/dashboard";
	}
}
