package com.example.settlement.web.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [NEW] Spring Security 권한 제어 통합 테스트
 *
 * @author gayul.kim
 * @since 2026-03-09
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("비로그인 사용자는 대시보드 접근 시 로그인 페이지로 리다이렉트되어야 한다")
    void nonLoginUser_accessDashboard_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("비로그인 사용자는 웰컴 페이지(/)에 접근 가능해야 한다")
    void nonLoginUser_accessWelcome_isOk() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = "SUPER_ADMIN")
    @DisplayName("SUPER_ADMIN 사용자는 관리자 페이지 접근이 가능해야 한다")
    void superAdmin_accessAdminUrl_isOk() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = "USER")
    @DisplayName("일반 사용자는 관리자 페이지 접근 시 403 Forbidden이 반환되어야 한다")
    void normalUser_accessAdminUrl_isForbidden() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isForbidden());
    }
}
