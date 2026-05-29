package com.example.settlement.demo;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * [NEW] 모든 뷰에 demoEnabled 플래그를 노출하는 ControllerAdvice.
 *
 * <p>
 * Thymeleaf 템플릿에서 ${demoEnabled} 로 데모 모드 활성화 여부를 조건부 렌더링에
 * 활용할 수 있도록 합니다. 로그인 페이지에서 데모 버튼 표시 여부를 결정합니다.
 * </p>
 *
 * @author gayul.kim
 * @since 2026-05-30
 */
@ControllerAdvice
@RequiredArgsConstructor
public class DemoViewAdvice {

	private final DemoProperties demoProperties;

	@ModelAttribute("demoEnabled")
	public boolean demoEnabled() {
		return demoProperties.isEnabled();
	}
}
