package com.example.settlement.controller;

import com.example.settlement.common.IpAddressExtractor;
import com.example.settlement.domain.entity.User;
import com.example.settlement.domain.repository.UserRepository;
import com.example.settlement.exception.EmailSendException;
import com.example.settlement.exception.EmailVerificationException;
import com.example.settlement.exception.RateLimitExceededException;
import com.example.settlement.service.email.EmailVerificationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

/**
 * [NEW] Email verification endpoints.
 *
 * <p>
 * - {@code GET /auth/verify-email/{token}} — handles the click-through link.
 * - {@code POST /auth/resend-verification}  — re-sends the verification email when
 *   the user requests it from the signup-success page.
 * </p>
 *
 * <p>
 * Both endpoints are public (configured in SecurityConfig); the only secret is the
 * 256-bit URL token, which is single-use and short-lived.
 * </p>
 *
 * @author gayul.kim
 * @since 2026-04-28
 */
@Slf4j
@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class EmailVerificationController {

	private final EmailVerificationService emailVerificationService;
	private final UserRepository userRepository;

	/**
	 * Handles the verification link clicked from the email.
	 *
	 * <p>
	 * GET is idempotent here only in the sense that re-clicking after success returns
	 * an "already verified" outcome rather than re-running the side effect.
	 * </p>
	 */
	@GetMapping("/verify-email/{token}")
	public String verifyEmail(@PathVariable String token, Model model) {
		try {
			User verified = emailVerificationService.verifyToken(token);
			model.addAttribute("status", "SUCCESS");
			model.addAttribute("email", verified.getEmail());
			model.addAttribute("message", "이메일 인증이 완료되었습니다. 관리자 승인 후 로그인이 가능합니다.");
		} catch (EmailVerificationException ex) {
			model.addAttribute("status", ex.getReason().name());
			model.addAttribute("message", ex.getMessage());
		}
		return "pages/auth/verify-email-result";
	}

	/**
	 * Re-sends the verification email. Caller submits the email address from the
	 * signup-success or login page.
	 *
	 * <p>
	 * Privacy: the response is intentionally identical regardless of whether the
	 * email exists, to avoid being a user-enumeration oracle. Rate limits still apply.
	 * </p>
	 */
	@PostMapping("/resend-verification")
	public String resendVerification(@RequestParam("email") String email,
			HttpServletRequest request,
			Model model) {
		String ipAddress = IpAddressExtractor.extract(request);
		String genericMessage = "인증 메일이 발송되었습니다. 메일함을 확인해주세요. (스팸 메일함도 함께 확인해주세요.)";

		try {
			Optional<User> userOpt = userRepository.findByEmail(email);
			if (userOpt.isEmpty()) {
				log.info("[AUDIT][RESEND_VERIFY] unknown_email email={} ip={}", email, ipAddress);
				model.addAttribute("status", "OK");
				model.addAttribute("message", genericMessage);
				return "pages/auth/verify-email-result";
			}

			User user = userOpt.get();
			if (Boolean.TRUE.equals(user.getEmailVerified())) {
				model.addAttribute("status", "ALREADY_VERIFIED");
				model.addAttribute("message", "이미 인증이 완료된 계정입니다. 로그인 화면에서 로그인해주세요.");
				return "pages/auth/verify-email-result";
			}

			emailVerificationService.sendVerificationEmail(user, ipAddress);
			model.addAttribute("status", "OK");
			model.addAttribute("message", genericMessage);
		} catch (RateLimitExceededException ex) {
			log.warn("[AUDIT][RESEND_VERIFY] rate_limited dimension={} email={} ip={}",
					ex.getDimension(), email, ipAddress);
			model.addAttribute("status", "RATE_LIMITED");
			model.addAttribute("message", ex.getMessage());
		} catch (EmailSendException ex) {
			log.error("[AUDIT][RESEND_VERIFY] send_failed email={} ip={} reason={}",
					email, ipAddress, ex.getMessage());
			model.addAttribute("status", "SEND_FAILED");
			model.addAttribute("message", "메일 발송 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
		}
		return "pages/auth/verify-email-result";
	}
}
