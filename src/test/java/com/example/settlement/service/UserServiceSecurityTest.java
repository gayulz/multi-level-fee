package com.example.settlement.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * [NEW] 사용자 관리 서비스 보안 제어 테스트
 *
 * @author gayul.kim
 * @since 2026-03-28
 */
@SpringBootTest
@Transactional
class UserServiceSecurityTest {

    @Autowired
    private UserService userService;

    @Test
    @DisplayName("일반 USER는 사용자 가입 승인 처리를 할 수 없어야 한다")
    @WithMockUser(roles = "USER")
    void approveUser_AccessDenied_For_USER() {
        // given
        Long targetUserId = 2L;
        Long approverId = 1L; // 자신이 USER라고 하더라도 승인 권한이 없음

        // when & then
        assertThatThrownBy(() -> userService.approveUser(targetUserId, approverId))
                .isInstanceOf(AccessDeniedException.class);
    }
}
