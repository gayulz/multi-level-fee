package com.example.settlement.controller;

import com.example.settlement.domain.entity.enums.UserRole;
import com.example.settlement.service.OrganizationService;
import com.example.settlement.service.UserService;
import com.example.settlement.web.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * [NEW] SUPER_ADMIN 전용 시스템 관리 컨트롤러.
 *
 * 사용자 가입 승인/반려, 권한 변경 기능을 담당합니다.
 *
 * @author gayul.kim
 * @since 2026-03-09
 */
@Slf4j
@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final OrganizationService organizationService;

    /**
     * [NEW] 사용자 관리 페이지.
     *
     * @author gayul.kim
     * @param model 뷰 모델
     * @return 사용자 관리 뷰 경로
     */
    @GetMapping("/users")
    public String userManagement(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable, 
            Model model) {
        model.addAttribute("currentPage", "admin-users");
        model.addAttribute("pageTitle", "사용자 관리");
        model.addAttribute("pendingUsers", userService.getAllPendingUsers());
        model.addAttribute("allUsers", userService.getAllUsers(pageable));
        return "pages/admin/user-management";
    }

    /**
     * [NEW] 사용자 가입 승인 처리 (POST).
     *
     * @author gayul.kim
     * @param id          승인할 사용자 ID
     * @param userDetails 인증된 SUPER_ADMIN 정보
     * @return 사용자 관리 페이지 리다이렉트
     */
    @PostMapping("/users/{id}/approve")
    public String approveUser(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        userService.approveUser(id, userDetails.getUser().getUserId());
        return "redirect:/admin/users";
    }

    /**
     * [NEW] 사용자 가입 반려 처리 (POST).
     *
     * @author gayul.kim
     * @param id          반려할 사용자 ID
     * @param reason      반려 사유
     * @param userDetails 인증된 SUPER_ADMIN 정보
     * @return 사용자 관리 페이지 리다이렉트
     */
    @PostMapping("/users/{id}/reject")
    public String rejectUser(
            @PathVariable Long id,
            @RequestParam("reason") String reason,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        userService.rejectUser(id, userDetails.getUser().getUserId(), reason);
        return "redirect:/admin/users";
    }

    /**
     * [NEW] 사용자 권한 변경 처리 (POST).
     *
     * @author gayul.kim
     * @param id   사용자 ID
     * @param role 변경할 권한
     * @return 사용자 관리 페이지 리다이렉트
     */
    @PostMapping("/users/{id}/role")
    public String changeRole(
            @PathVariable Long id,
            @RequestParam("role") UserRole role) {

        userService.changeUserRole(id, role);
        return "redirect:/admin/users";
    }
}
