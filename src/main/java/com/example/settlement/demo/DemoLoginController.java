package com.example.settlement.demo;

import com.example.settlement.web.security.CustomUserDetails;
import com.example.settlement.web.security.CustomUserDetailsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import static com.example.settlement.demo.DemoConstants.*;

/**
 * [NEW] 데모 자동 로그인 엔드포인트.
 *
 * <p>
 * 클라이언트 측에 비밀번호를 노출하지 않고 서버에서 직접 SecurityContext 를 채워
 * 세션에 저장합니다. role 파라미터로 4종 데모 계정 중 하나를 선택합니다.
 * </p>
 *
 * <p>
 * 보안 가드:
 * - app.demo.enabled=false 면 모든 요청을 401 로 거부합니다.
 * - 데모 계정이 아닌 일반 계정으로 위조 로그인 시도를 막기 위해, demo_ prefix 계정만
 *   허용합니다.
 * </p>
 *
 * @author gayul.kim
 * @since 2026-05-30
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class DemoLoginController {

	private final CustomUserDetailsService userDetailsService;
	private final DemoProperties demoProperties;

	private static final String SPRING_SECURITY_CONTEXT_KEY = "SPRING_SECURITY_CONTEXT";

	@PostMapping("/demo-login")
	public Object demoLogin(@RequestParam("role") String role,
			HttpServletRequest request, HttpServletResponse response) {
		if (!demoProperties.isEnabled()) {
			log.warn("[DemoLoginController] demo disabled - reject login");
			return ResponseEntity.status(404).body("Not Found");
		}

		String email = resolveEmail(role);
		if (email == null) {
			return ResponseEntity.badRequest().body("Unknown demo role: " + role);
		}

		CustomUserDetails details;
		try {
			details = (CustomUserDetails) userDetailsService.loadUserByUsername(email);
		} catch (Exception e) {
			log.warn("[DemoLoginController] demo account not found: {} ({})", email, e.getMessage());
			return ResponseEntity.status(404).body("Demo account not seeded. Set app.demo.enabled=true and restart.");
		}

		Authentication auth = new UsernamePasswordAuthenticationToken(
				details, null, details.getAuthorities());
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(auth);
		SecurityContextHolder.setContext(context);

		// Persist the SecurityContext to the session so subsequent requests are authenticated
		HttpSession session = request.getSession(true);
		session.setAttribute(SPRING_SECURITY_CONTEXT_KEY, context);

		log.info("[DemoLoginController] demo login success: email={}, role={}", email, role);
		return new RedirectView("/dashboard");
	}

	/**
	 * [NEW] Map the UI role keyword to the seeded demo account email.
	 *
	 * @param role one of super | hq | branch | agency
	 * @return demo email or null if unknown
	 * @author gayul.kim
	 */
	private String resolveEmail(String role) {
		if (role == null) return null;
		return switch (role.toLowerCase()) {
			case "super" -> DEMO_SUPER_EMAIL;
			case "hq" -> DEMO_HQ_EMAIL;
			case "branch" -> DEMO_BRANCH_EMAIL;
			case "agency" -> DEMO_AGENCY_EMAIL;
			default -> null;
		};
	}
}
