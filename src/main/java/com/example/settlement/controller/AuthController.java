package com.example.settlement.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * [NEW] 인증(로그인/회원가입) 페이지 렌더링 Controller
 *
 * 웰컴 페이지에서 진입하는 로그인 및 회원가입 페이지 정적 뷰를 반환합니다.
 *
 * @author gayul.kim
 * @since 2026-03-06
 */
@Controller
public class AuthController {

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
    public String signup() {
        return "pages/auth/signup";
    }
}
