package com.example.settlement.web.controller;

import com.example.settlement.common.PasswordPolicy;
import com.example.settlement.domain.entity.User;
import com.example.settlement.service.UserService;
import com.example.settlement.web.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * [NEW] 설정 컨트롤러
 *
 * 개인정보 수정, 비밀번호 변경 등을 처리한다.
 *
 * @author gayul.kim
 * @since 2025-01-30
 */
@Controller
@RequestMapping("/settings")
@RequiredArgsConstructor
public class SettingsController {

	private final UserService userService;

	/**
	 * 개인정보 수정 화면
	 *
	 * @param userDetails 현재 로그인한 사용자
	 * @param model 뷰 모델
	 * @return 개인정보 수정 템플릿
	 */
	@GetMapping("/profile")
	public String profilePage(
		@AuthenticationPrincipal CustomUserDetails userDetails,
		Model model
	) {
		User user = userDetails.getUser();

		model.addAttribute("pageTitle", "개인정보 수정");
		model.addAttribute("user", user);
		model.addAttribute("pwMinLength", PasswordPolicy.MIN_LENGTH);
		model.addAttribute("pwHtmlPattern", PasswordPolicy.HTML_PATTERN);
		model.addAttribute("pwInputTitle", PasswordPolicy.INPUT_TITLE);
		model.addAttribute("pwViolationMessage", PasswordPolicy.VIOLATION_MESSAGE);

		return "pages/settings/profile";
	}

	/**
	 * 개인정보 수정 처리 (이름, 연락처)
	 *
	 * @param userDetails 현재 로그인한 사용자
	 * @param name 수정할 이름
	 * @param phone 수정할 연락처
	 * @param redirectAttributes 리다이렉트 속성
	 * @return 리다이렉트 경로
	 */
	@PostMapping("/profile")
	public String updateProfile(
		@AuthenticationPrincipal CustomUserDetails userDetails,
		@RequestParam String name,
		@RequestParam String phone,
		RedirectAttributes redirectAttributes
	) {
		try {
			userService.updateProfile(userDetails.getUser().getUserId(), name, phone);
			redirectAttributes.addFlashAttribute("message", "개인정보가 수정되었습니다.");
			redirectAttributes.addFlashAttribute("alertType", "success");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("message", "개인정보 수정에 실패했습니다: " + e.getMessage());
			redirectAttributes.addFlashAttribute("alertType", "error");
		}

		return "redirect:/settings/profile";
	}

	/**
	 * 비밀번호 변경 처리
	 *
	 * @param userDetails 현재 로그인한 사용자
	 * @param currentPassword 현재 비밀번호
	 * @param newPassword 새 비밀번호
	 * @param confirmPassword 새 비밀번호 확인
	 * @param redirectAttributes 리다이렉트 속성
	 * @return 리다이렉트 경로
	 */
	@PostMapping("/change-password")
	public String changePassword(
		@AuthenticationPrincipal CustomUserDetails userDetails,
		@RequestParam String currentPassword,
		@RequestParam String newPassword,
		@RequestParam String confirmPassword,
		RedirectAttributes redirectAttributes
	) {
		try {
			// 새 비밀번호 일치 확인
			if (!newPassword.equals(confirmPassword)) {
				redirectAttributes.addFlashAttribute("message", "새 비밀번호가 일치하지 않습니다.");
				redirectAttributes.addFlashAttribute("alertType", "error");
				return "redirect:/settings/profile";
			}

			// 비밀번호 정책 검증 (PasswordPolicy 단일 소스)
			if (!PasswordPolicy.isValid(newPassword)) {
				redirectAttributes.addFlashAttribute("message", PasswordPolicy.VIOLATION_MESSAGE);
				redirectAttributes.addFlashAttribute("alertType", "error");
				return "redirect:/settings/profile";
			}

			userService.changePassword(userDetails.getUser().getUserId(), currentPassword, newPassword);
			redirectAttributes.addFlashAttribute("message", "비밀번호가 변경되었습니다.");
			redirectAttributes.addFlashAttribute("alertType", "success");
		} catch (IllegalArgumentException e) {
			// Service 레이어에서 던지는 검증 실패 메시지(현재 비밀번호 불일치 등)만 노출
			redirectAttributes.addFlashAttribute("message", e.getMessage());
			redirectAttributes.addFlashAttribute("alertType", "error");
		} catch (Exception e) {
			// 시스템 오류는 일반 메시지만 노출 (스택트레이스 누수 방지)
			redirectAttributes.addFlashAttribute("message", "비밀번호 변경 중 오류가 발생했습니다.");
			redirectAttributes.addFlashAttribute("alertType", "error");
		}

		return "redirect:/settings/profile";
	}

	/**
	 * [NEW] 모달 팝업용 비밀번호 변경 API
	 *
	 * @param userDetails 현재 로그인한 사용자
	 * @param currentPassword 현재 비밀번호
	 * @param newPassword 새 비밀번호
	 * @param confirmPassword 새 비밀번호 확인
	 * @return JSON 응답
	 * @author gayul.kim
	 * @since 2026-04-21
	 */
	@PostMapping("/api/change-password")
	@org.springframework.web.bind.annotation.ResponseBody
	public org.springframework.http.ResponseEntity<?> changePasswordApi(
		@AuthenticationPrincipal CustomUserDetails userDetails,
		@RequestParam String currentPassword,
		@RequestParam String newPassword,
		@RequestParam String confirmPassword
	) {
		try {
			// 새 비밀번호 일치 확인
			if (!newPassword.equals(confirmPassword)) {
				return org.springframework.http.ResponseEntity.badRequest().body(java.util.Map.of("message", "새 비밀번호가 일치하지 않습니다."));
			}

			// 비밀번호 복잡도 검증 (PasswordPolicy 단일 소스 참조)
			if (!PasswordPolicy.isValid(newPassword)) {
				return org.springframework.http.ResponseEntity.badRequest().body(java.util.Map.of("message", PasswordPolicy.VIOLATION_MESSAGE));
			}

			userService.changePassword(userDetails.getUser().getUserId(), currentPassword, newPassword);
			return org.springframework.http.ResponseEntity.ok(java.util.Map.of("message", "비밀번호가 성공적으로 변경되었습니다."));
		} catch (IllegalArgumentException e) {
			return org.springframework.http.ResponseEntity.badRequest().body(java.util.Map.of("message", e.getMessage()));
		} catch (Exception e) {
			return org.springframework.http.ResponseEntity.internalServerError().body(java.util.Map.of("message", "비밀번호 변경에 실패했습니다."));
		}
	}
}
