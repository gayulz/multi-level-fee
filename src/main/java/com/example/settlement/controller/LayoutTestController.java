package com.example.settlement.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * [NEW] 레이아웃 테스트 Controller
 *
 * 공통 레이아웃이 정상적으로 렌더링되는지 테스트하기 위한 임시 Controller입니다.
 *
 * @author gayul.kim
 * @since 2026-02-21
 */
@Controller
public class LayoutTestController {

	/**
	 * 레이아웃 테스트 페이지
	 *
	 * @param model Model 객체
	 * @return 레이아웃 테스트 페이지 뷰 이름
	 */
	@GetMapping("/layout-test")
	public String layoutTest(Model model) {
		model.addAttribute("pageTitle", "레이아웃 테스트");
		model.addAttribute("currentPage", "layout-test");
		return "pages/layout-test";
	}
}
