package com.example.settlement.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * [NEW] 웰컴 페이지 Controller
 *
 * 상용화 업그레이드를 위한 첫 번째 진입점인 Welcome 페이지를 렌더링합니다.
 * Spline 3D 기반 인터랙티브 UI를 반환합니다.
 *
 * @author gayul.kim
 * @since 2026-03-06
 */
@Controller
public class WelcomeController {

    /**
     * 메인 웰컴 페이지
     * 루트(/) 접속 시 웰컴 페이지를 응답합니다.
     *
     * @return 웰컴 페이지 뷰 템플릿 이름
     */
    @GetMapping("/")
    public String welcome() {
        return "pages/welcome";
    }
}
