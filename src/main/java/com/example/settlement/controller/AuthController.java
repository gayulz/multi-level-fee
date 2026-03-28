package com.example.settlement.controller;

import com.example.settlement.dto.request.SignupRequest;
import com.example.settlement.service.OrganizationService;
import com.example.settlement.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final OrganizationService organizationService;
    private final UserService userService;

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
        return "pages/auth/signup";
    }

    /**
     * [NEW] 회원가입 처리 로직
     *
     * @author gayul.kim
     */
    @PostMapping("/signup")
    public String signup(@Valid @ModelAttribute SignupRequest request,
            BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("organizations", organizationService.getAllOrganizations());
            return "pages/auth/signup";
        }

        try {
            userService.registerUser(request);
            // TODO: 성공 여부에 따라 파라미터나 전용 페이지(auth/signup/success)로 리다이렉트 (임시로 로그인 페이지로 이동)
            return "redirect:/auth/login?registered=true";
        } catch (IllegalArgumentException e) {
            bindingResult.rejectValue("email", "duplicate", e.getMessage());
            model.addAttribute("organizations", organizationService.getAllOrganizations());
            return "pages/auth/signup";
        }
    }
}
