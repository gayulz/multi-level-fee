package com.example.settlement.controller;

import com.example.settlement.common.IpAddressExtractor;
import com.example.settlement.common.PasswordPolicy;
import com.example.settlement.domain.entity.User;
import com.example.settlement.dto.request.SignupRequest;
import com.example.settlement.exception.EmailSendException;
import com.example.settlement.exception.RateLimitExceededException;
import com.example.settlement.service.OrganizationService;
import com.example.settlement.service.UserService;
import com.example.settlement.service.email.EmailVerificationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * [NEW] 인증(로그인/회원가입) 페이지 렌더링 Controller
 *
 * 웰컴 페이지에서 진입하는 로그인 및 회원가입 페이지 정적 뷰를 반환합니다.
 *
 * @author gayul.kim
 * @since 2026-03-06
 */
@Slf4j
@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final OrganizationService organizationService;
    private final UserService userService;
    private final EmailVerificationService emailVerificationService;

    /**
     * [NEW] 로그인 뷰 페이지 이동
     *
     * @author gayul.kim
     * @return 로그인 페이지 (pages/auth/login.html)
     */
    @GetMapping("/login")
    public String login() {
        return "pages/auth/login";
    }

    /**
     * [NEW] 회원가입 뷰 페이지 이동
     *
     * @author gayul.kim
     * @return 회원가입 페이지 (pages/auth/signup.html)
     */
    @GetMapping("/signup")
    public String signupForm(Model model) {
        model.addAttribute("signupRequest", new SignupRequest());
        model.addAttribute("organizations", organizationService.getAllOrganizations());
        addPasswordPolicy(model);
        return "pages/auth/signup";
    }

    /**
     * [NEW] 회원가입 성공 뷰 페이지 이동
     *
     * @author gayul.kim
     * @return 가입 완료 안내 페이지
     */
    @GetMapping("/signup/success")
    public String signupSuccess() {
        return "pages/auth/signup-success";
    }

    /**
     * [NEW] 회원가입 처리 로직
     *
     * @author gayul.kim
     */
    @PostMapping("/signup")
    public String signup(@Valid @ModelAttribute SignupRequest request,
            BindingResult bindingResult,
            HttpServletRequest httpRequest,
            Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("organizations", organizationService.getAllOrganizations());
            addPasswordPolicy(model);
            return "pages/auth/signup";
        }

        try {
            User registered = userService.registerUser(request);
            // [NEW] Registration is committed; verification email is fire-and-best-effort.
            // A send failure must not roll back the user record — instead surface a banner
            // on the success page so the user can re-trigger via "resend".
            try {
                String ip = IpAddressExtractor.extract(httpRequest);
                emailVerificationService.sendVerificationEmail(registered, ip);
                model.addAttribute("emailDispatched", true);
            } catch (RateLimitExceededException ex) {
                log.warn("[AUDIT][SIGNUP] rate_limited dimension={} email={}",
                        ex.getDimension(), registered.getEmail());
                model.addAttribute("emailDispatched", false);
                model.addAttribute("emailWarning", ex.getMessage());
            } catch (EmailSendException ex) {
                log.error("[AUDIT][SIGNUP] verification_send_failed email={} reason={}",
                        registered.getEmail(), ex.getMessage());
                model.addAttribute("emailDispatched", false);
                model.addAttribute("emailWarning",
                        "인증 메일 발송 중 오류가 발생했습니다. 가입 화면 하단의 [재발송] 기능으로 다시 시도해주세요.");
            }
            model.addAttribute("registeredEmail", registered.getEmail());
            return "pages/auth/signup-success";
        } catch (IllegalArgumentException e) {
            bindingResult.rejectValue("email", "duplicate", e.getMessage());
            model.addAttribute("organizations", organizationService.getAllOrganizations());
            addPasswordPolicy(model);
            return "pages/auth/signup";
        }
    }

    /**
     * Exposes {@link PasswordPolicy} constants to the view as model attributes.
     *
     * <p>
     * Direct {@code T(...)} static access from Thymeleaf is blocked by SpringSecurity's
     * SimpleEvaluationContext on form-backing pages, so we surface the constants explicitly.
     * </p>
     */
    private void addPasswordPolicy(Model model) {
        model.addAttribute("pwMinLength", PasswordPolicy.MIN_LENGTH);
        model.addAttribute("pwHtmlPattern", PasswordPolicy.HTML_PATTERN);
        model.addAttribute("pwInputTitle", PasswordPolicy.INPUT_TITLE);
        model.addAttribute("pwViolationMessage", PasswordPolicy.VIOLATION_MESSAGE);
    }
}
