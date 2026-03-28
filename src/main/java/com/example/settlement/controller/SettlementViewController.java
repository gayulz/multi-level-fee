package com.example.settlement.controller;

import com.example.settlement.domain.entity.User;
import com.example.settlement.domain.entity.enums.UserRole;
import com.example.settlement.dto.request.SettlementRequestDto;
import com.example.settlement.service.ApprovalService;
import com.example.settlement.service.SettlementService;
import com.example.settlement.web.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * [NEW] 정산 요청/결과 뷰 컨트롤러.
 *
 * 정산 요청 폼, 정산 내역 목록, 정산 상세 페이지 렌더링과
 * 정산 요청 POST, 승인/반려 POST를 처리합니다.
 *
 * @author gayul.kim
 * @since 2026-03-09
 */
@Slf4j
@Controller
@RequestMapping("/settlement")
@RequiredArgsConstructor
public class SettlementViewController {

    private final SettlementService settlementService;
    private final ApprovalService approvalService;

    /**
     * [NEW] 정산 요청 폼 페이지.
     *
     * @author gayul.kim
     * @param model Model 객체
     * @return templates/pages/settlement/request.html
     */
    @GetMapping("/request")
    public String requestForm(Model model) {
        model.addAttribute("currentPage", "settlement-request");
        model.addAttribute("pageTitle", "정산 요청");
        model.addAttribute("rootNodes", settlementService.getRootNodes());
        return "pages/settlement/request";
    }

    /**
     * [NEW] 정산 요청 처리 (POST).
     *
     * @author gayul.kim
     * @param orderId     주문 ID
     * @param amount      정산 금액
     * @param rootNodeId  루트 노드 ID
     * @param description 설명
     * @param userDetails 인증된 사용자
     * @return 정산 내역 페이지 리다이렉트
     */
    @PostMapping("/request")
    public String createRequest(
            @RequestParam("orderId") String orderId,
            @RequestParam("amount") BigDecimal amount,
            @RequestParam("rootNodeId") Long rootNodeId,
            @RequestParam(value = "description", required = false) String description,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        SettlementRequestDto dto = new SettlementRequestDto(orderId, amount, rootNodeId, description);
        settlementService.createRequest(dto, userDetails.getUser());
        return "redirect:/settlement/history";
    }

    /**
     * [NEW] 정산 내역 목록 페이지.
     *
     * ADMIN 이상: 소속 조직의 전체 정산 내역.
     * USER: 본인 정산 내역만.
     *
     * @author gayul.kim
     * @param userDetails 인증된 사용자
     * @param model       Model 객체
     * @return templates/pages/settlement/history.html
     */
    @GetMapping("/history")
    public String history(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        model.addAttribute("currentPage", "settlement-history");
        model.addAttribute("pageTitle", "정산 내역");

        User user = userDetails.getUser();
        if (user.hasRole(UserRole.ROLE_ADMIN) || user.hasRole(UserRole.ROLE_SUPER_ADMIN)) {
            model.addAttribute("settlements",
                    settlementService.getRequestsByOrganization(user.getOrganization().getOrgId()));
        } else {
            model.addAttribute("settlements", settlementService.getRequestsByUser(user));
        }

        return "pages/settlement/history";
    }

    /**
     * [NEW] 정산 상세 결과 페이지.
     *
     * @author gayul.kim
     * @param id    정산 건 ID
     * @param model Model 객체
     * @return templates/pages/settlement/detail.html
     */
    @GetMapping("/detail/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("currentPage", "settlement-history");
        model.addAttribute("pageTitle", "정산 상세");
        model.addAttribute("request", settlementService.getRequest(id));
        return "pages/settlement/detail";
    }

    /**
     * [NEW] 정산 승인 처리 (POST).
     *
     * @author gayul.kim
     * @param id          정산 요청 ID
     * @param comment     승인 코멘트
     * @param userDetails 인증된 사용자 (승인자)
     * @return 대시보드 리다이렉트
     */
    @PostMapping("/approve/{id}")
    public String approve(
            @PathVariable Long id,
            @RequestParam(value = "comment", required = false, defaultValue = "") String comment,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        approvalService.approve(id, userDetails.getUser(), comment);
        return "redirect:/dashboard";
    }

    /**
     * [NEW] 정산 반려 처리 (POST).
     *
     * @author gayul.kim
     * @param id          정산 요청 ID
     * @param reason      반려 사유
     * @param userDetails 인증된 사용자 (반려자)
     * @return 대시보드 리다이렉트
     */
    @PostMapping("/reject/{id}")
    public String reject(
            @PathVariable Long id,
            @RequestParam("reason") String reason,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        approvalService.reject(id, userDetails.getUser(), reason);
        return "redirect:/dashboard";
    }
}
