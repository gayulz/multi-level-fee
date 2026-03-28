package com.example.settlement.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * [NEW] 커스텀 에러 컨트롤러
 *
 * Spring Boot 기본 Whitelabel Error Page 대신
 * 커스텀 에러 페이지를 렌더링합니다.
 *
 * @author gayul.kim
 * @since 2026-03-04
 */
@Controller
public class CustomErrorController implements ErrorController {

    /**
     * [NEW] 에러 페이지 핸들러
     *
     * HTTP 상태 코드에 따라 적절한 에러 페이지를 반환합니다.
     * 404 → error/404.html, 500 → error/500.html, 기타 → error/error.html
     *
     * @author gayul.kim
     * @param request HttpServletRequest 객체
     * @param model   Model 객체
     * @return 에러 페이지 뷰 이름
     */
    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Object errorType = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION_TYPE);
        Object path = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);

        model.addAttribute("status", status);
        model.addAttribute("message", message);
        model.addAttribute("error", errorType);
        model.addAttribute("path", path);

        if (status != null) {
            int statusCode = Integer.parseInt(status.toString());

            if (statusCode == HttpStatus.NOT_FOUND.value()) {
                return "error/404";
            } else if (statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                return "error/500";
            }
        }

        return "error/error";
    }

    /**
     * [NEW] 403 Forbidden 에러 페이지 핸들러
     *
     * SecurityConfig에서 accessDeniedPage로 설정된 경로를 처리합니다.
     *
     * @author gayul.kim
     * @return 403 에러 페이지 뷰 이름
     */
    @RequestMapping("/error/403")
    public String accessDenied() {
        return "error/403";
    }
}
