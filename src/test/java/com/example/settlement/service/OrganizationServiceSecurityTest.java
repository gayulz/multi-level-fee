package com.example.settlement.service;

import com.example.settlement.domain.entity.enums.OrgType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * [NEW] 조직 관리 서비스 보안 제어 테스트
 *
 * @author gayul.kim
 * @since 2026-03-28
 */
@SpringBootTest
@Transactional
class OrganizationServiceSecurityTest {

    @Autowired
    private OrganizationService organizationService;

    @Test
    @DisplayName("SUPER_ADMIN이 아닌 사용자가 조직 생성 시도 시 예외가 발생한다")
    @WithMockUser(roles = {"USER", "ADMIN"})
    void createOrganization_AccessDenied() {
        // given
        String name = "Test Branch";
        OrgType type = OrgType.BRANCH;
        Long parentId = 1L;

        // when & then
        assertThatThrownBy(() -> organizationService.createOrganization(name, type, parentId))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("로그인한 사용자는 조직 목록 조회 권한이 있다")
    @WithMockUser(roles = "USER")
    void getAllOrganizations_Success_ForAuthenticatedUser() {
        // when & then
        assertDoesNotThrow(() -> organizationService.getAllOrganizations());
    }
}
