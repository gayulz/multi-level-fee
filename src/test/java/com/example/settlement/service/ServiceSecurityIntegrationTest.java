package com.example.settlement.service;

import com.example.settlement.domain.entity.Organization;
import com.example.settlement.domain.entity.User;
import com.example.settlement.domain.entity.enums.OrgType;
import com.example.settlement.domain.entity.enums.UserRole;
import com.example.settlement.web.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;

/**
 * [NEW] 서비스 계층 보안(@PreAuthorize) 통합 테스트
 *
 * @author gayul.kim
 * @since 2026-03-28
 */
@SpringBootTest
@Transactional
class ServiceSecurityIntegrationTest {

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private UserService userService;

    @Autowired
    private SettlementService settlementService;

    @MockBean
    private RabbitTemplate rabbitTemplate; // 인프라 의존성 모킹

    @Test
    @DisplayName("ROLE_USER 권한으로는 조직을 생성할 수 없다 (AccessDeniedException 발생)")
    @WithMockUser(roles = "USER")
    void createOrganization_deniedForUser() {
        assertThatThrownBy(() -> 
            organizationService.createOrganization("신규 본사", OrgType.HEADQUARTERS, null)
        ).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("ROLE_SUPER_ADMIN 권한으로는 조직을 생성할 수 있다")
    @WithMockUser(roles = "SUPER_ADMIN")
    void createOrganization_allowedForSuperAdmin() {
        organizationService.createOrganization("신규 본사", OrgType.HEADQUARTERS, null);
    }

    @Test
    @DisplayName("ROLE_ADMIN 권한으로는 사용자 권한을 변경할 수 없다")
    @WithMockUser(roles = "ADMIN")
    void changeUserRole_deniedForAdmin() {
        assertThatThrownBy(() -> 
            userService.changeUserRole(1L, UserRole.ROLE_SUPER_ADMIN)
        ).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("ROLE_SUPER_ADMIN 권한으로는 사용자 권한을 변경할 수 있다")
    @WithMockUser(roles = "SUPER_ADMIN")
    void changeUserRole_allowedForSuperAdmin() {
        // 실제 유저가 DB에 있어야 하므로, 실제로는 mock id를 주거나 사전 데이터를 준비해야 함
        // 여기서는 @PreAuthorize 어노테이션 진입 단계의 권한만 체크하므로 에러 메시지보다는 예외 타입에 집중
        try {
            userService.changeUserRole(999L, UserRole.ROLE_ADMIN);
        } catch (IllegalArgumentException e) {
            // 권한 통과 후 유저가 없는 에러이므로 성공으로 간주
        }
    }

    @Test
    @DisplayName("일반 사용자는 정산 계산(calculate)을 수행할 수 없다")
    @WithMockUser(roles = "USER")
    void calculate_deniedForUser() {
        assertThatThrownBy(() -> 
            settlementService.calculate(null) // DTO가 null이어도 권한 체크를 먼저 함
        ).isInstanceOf(AccessDeniedException.class);
    }
}
