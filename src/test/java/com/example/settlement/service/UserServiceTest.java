package com.example.settlement.service;

import com.example.settlement.domain.entity.Organization;
import com.example.settlement.domain.entity.User;
import com.example.settlement.domain.entity.enums.OrgType;
import com.example.settlement.domain.entity.enums.UserRole;
import com.example.settlement.domain.entity.enums.UserStatus;
import com.example.settlement.dto.request.SignupRequest;
import com.example.settlement.web.security.CustomUserDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * [NEW] 사용자 관리 서비스 통합 시나리오 테스트
 * (가입부터 권한별 승인 워크플로우까지 검증)
 *
 * @author gayul.kim
 * @since 2026-03-28
 */
@SpringBootTest
@Transactional
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private OrganizationService organizationService;

    /**
     * SecurityContext를 수동으로 조작하여 주어진 User 엔티티 기반으로 로그인된 상태를 만듭니다.
     */
    private void setAuthentication(User user) {
        CustomUserDetails userDetails = new CustomUserDetails(user);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
    
    /**
     * 단순히 문자열 이메일과 권한만으로 Fake 인증을 만드는 헬퍼 (초기 셋업용)
     */
    private void setFakeSuperAdminAuthentication() {
        User superAdmin = User.createSuperAdmin(
                "system_setup@test.com",
                "Password123!",
                "System Admin",
                "010-0000-0000",
                null
        );
        ReflectionTestUtils.setField(superAdmin, "userId", 999L);
        setAuthentication(superAdmin);
    }
    /**
     * SecurityContext를 비워 비인증(Guest) 상태로 만듭니다.
     */
    private void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("사용자 가입(PENDING) 후, 읿반 사용자 권한으로 승인 시도 시 차단되고, 관리자 권한일 때 정상 승인(APPROVED)된다")
    void registerAndApproveUserWorkflowWithSecurity() {
        // [1] 시스템 셋업을 위해 강제로 최고 관리자(SUPER_ADMIN) 권한 부여
        setFakeSuperAdminAuthentication();
        
        Organization org = organizationService.createOrganization("테스트 본사", OrgType.HEADQUARTERS, null);

        SignupRequest approverReq = new SignupRequest();
        approverReq.setEmail("admin_approver@test.com");
        approverReq.setPassword("Password123!");
        approverReq.setName("관리자");
        approverReq.setPhone("010-0000-0000");
        approverReq.setOrganizationId(org.getOrgId());
        User approver = userService.registerUser(approverReq);
        
        // [1-1] 이제 일반 사용자 신규 가입을 위해 권한 초기화(Guest)
        clearAuthentication();
        
        // 관리자로 활용하기 위해 권한 강제 셋업 (테스트 목적)
        // 실제로는 별도의 SuperAdmin이 승인해줘야 하지만 여기서는 테스트 픽스처로 활용
        
        SignupRequest req = new SignupRequest();
        req.setEmail("new_user@test.com");
        req.setPassword("Password123!");
        req.setName("홍길동");
        req.setPhone("010-1234-5678");
        req.setOrganizationId(org.getOrgId());

        // When (Step 1): 사용자 가입 요청
        User newUser = userService.registerUser(req);

        // Then (Step 1): PENDING 상태 확인
        assertThat(newUser.getStatus()).isEqualTo(UserStatus.PENDING);

        // When (Step 2): ROLE_USER 권한으로 로그인했다고 가정하고 자신이/타인이 승인 시도
        // newUser는 생성될 때 ROLE_USER 권한을 부여받음
        setAuthentication(newUser);
        
        // Then (Step 2): AccessDeniedException 차단 확인 (@PreAuthorize 검증)
        assertThatThrownBy(() -> userService.approveUser(newUser.getUserId(), approver.getUserId()))
                .isInstanceOf(AccessDeniedException.class);

        // When (Step 3): ROLE_ADMIN 권한을 가진 관리자로 로그인했다고 가정
        approver.changeRole(UserRole.ROLE_ADMIN); // 테스트 목적 권한 부여
        setAuthentication(approver);
        
        // Then (Step 3): 정상 승인 처리 통과
        userService.approveUser(newUser.getUserId(), approver.getUserId());

        // 최종 상태 변경 확인을 위해 대상 사용자(newUser) 스스로 로그인했다고 가정
        // (관리자라 해도 다른 사용자 ID를 getUserById로 조회하려면 본사(SUPER_ADMIN) 관리자여야 함)
        setAuthentication(newUser);
        
        User approvedUser = userService.getUserById(newUser.getUserId());
        assertThat(approvedUser.getStatus()).isEqualTo(UserStatus.APPROVED);
        assertThat(approvedUser.getApprovedBy().getUserId()).isEqualTo(approver.getUserId());
    }
}
