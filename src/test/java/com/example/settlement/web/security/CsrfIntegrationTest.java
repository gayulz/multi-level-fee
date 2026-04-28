package com.example.settlement.web.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [NEW] CSRF protection integration tests.
 *
 * <p>
 * Verifies that Spring Security's CSRF policy correctly:
 * - blocks state-changing requests without a valid CSRF token (fail-closed)
 * - allows requests with a valid CSRF token
 * - exempts safe (idempotent) HTTP methods (GET) from CSRF checks
 * </p>
 *
 * @author gayul.kim
 * @since 2026-04-28
 */
@SpringBootTest
@AutoConfigureMockMvc
class CsrfIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("CSRF 토큰 없는 POST 요청은 403 Forbidden 이어야 한다")
	void postWithoutCsrf_isForbidden() throws Exception {
		mockMvc.perform(post("/auth/signup")
						.param("email", "test@settletree.io")
						.param("password", "Password1!")
						.param("name", "TestUser")
						.param("phone", "010-1234-5678")
						.param("organizationId", "1"))
				.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("CSRF 토큰이 있는 POST 요청은 통과해야 한다 (검증 단계 진입)")
	void postWithCsrf_passesCsrfFilter() throws Exception {
		// CSRF 통과 시점에서 비즈니스 검증 결과는 무관 — CSRF 필터만 통과하는지 확인
		// 잘못된 organizationId로 인해 4xx가 나더라도 403(CSRF 거부)이 아니어야 함
		mockMvc.perform(post("/auth/signup")
						.with(csrf())
						.param("email", "test@settletree.io")
						.param("password", "Password1!")
						.param("name", "TestUser")
						.param("phone", "010-1234-5678")
						.param("organizationId", "999999"))
				.andExpect(result -> {
					int code = result.getResponse().getStatus();
					if (code == 403) {
						throw new AssertionError("CSRF 필터에서 403이 발생했습니다 — 토큰이 정상 적용되지 않음");
					}
				});
	}

	@Test
	@DisplayName("GET 요청은 CSRF 검증 대상이 아니어야 한다 (idempotent)")
	void getRequest_csrfNotEnforced() throws Exception {
		mockMvc.perform(get("/auth/login"))
				.andExpect(status().isOk());
	}

	@Test
	@WithMockUser(username = "admin@test.com", roles = "SUPER_ADMIN")
	@DisplayName("인증된 사용자도 CSRF 토큰 없이는 POST 거부되어야 한다")
	void authenticatedUser_postWithoutCsrf_isForbidden() throws Exception {
		mockMvc.perform(post("/admin/users/1/approve"))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = "admin@test.com", roles = "SUPER_ADMIN")
	@DisplayName("인증된 사용자가 CSRF 토큰을 포함하면 CSRF 필터를 통과해야 한다 — logout 표준 엔드포인트")
	void authenticatedUser_postWithCsrf_passesCsrfFilter() throws Exception {
		// /auth/logout 은 Spring Security 표준 엔드포인트로 핸들러 분기 부담 없이
		// CSRF 필터 통과 여부만 검증하기에 가장 안전하다.
		mockMvc.perform(post("/auth/logout").with(csrf()))
				.andExpect(result -> {
					int code = result.getResponse().getStatus();
					if (code == 403) {
						throw new AssertionError("CSRF 필터에서 403이 발생했습니다 — 토큰이 정상 적용되지 않음");
					}
				});
	}
}
