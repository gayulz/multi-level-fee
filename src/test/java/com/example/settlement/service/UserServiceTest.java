package com.example.settlement.service;

import com.example.settlement.domain.entity.Organization;
import com.example.settlement.domain.entity.User;
import com.example.settlement.domain.entity.enums.OrgType;
import com.example.settlement.domain.entity.enums.UserStatus;
import com.example.settlement.dto.request.SignupRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [NEW] 사용자 관리 서비스 통합 테스트
 *
 * @author gayul.kim
 * @since 2026-03-09
 */
@SpringBootTest
@Transactional
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private OrganizationService organizationService;

    @Test
    @DisplayName("사용자 회원가입 시 PENDING 상태로 생성되고, 승인 시 APPROVED로 변경된다")
    void registerAndApproveUser() {
        // Given: 조직 생성 및 관리자(승인자) 생성
        Organization org = organizationService.createOrganization("테스트 본사", OrgType.HEADQUARTERS, null);

        SignupRequest approverReq = new SignupRequest();
        approverReq.setEmail("admin_approver@test.com");
        approverReq.setPassword("Password123!");
        approverReq.setName("관리자");
        approverReq.setPhone("010-0000-0000");
        approverReq.setOrganizationId(org.getOrgId());
        User approver = userService.registerUser(approverReq);
        // Force approver to be approved for test directly from DB point or we can just
        // mock it,
        // but since we are testing approveUser(userId, approverId) we can just use the
        // approver user regardless of its status for now.
        // Actually UserService approveUser uses approver. let's just use it.

        SignupRequest req = new SignupRequest();
        req.setEmail("new_user@test.com");
        req.setPassword("Password123!");
        req.setName("홍길동");
        req.setPhone("010-1234-5678");
        req.setOrganizationId(org.getOrgId());

        // When: 사용자 가입
        User newUser = userService.registerUser(req);

        // Then: PENDING 상태 확인
        assertThat(newUser.getStatus()).isEqualTo(UserStatus.PENDING);

        // When: 관리자 승인
        userService.approveUser(newUser.getUserId(), approver.getUserId());

        // Then: APPROVED 상태 확인
        User approvedUser = userService.getUserById(newUser.getUserId());
        assertThat(approvedUser.getStatus()).isEqualTo(UserStatus.APPROVED);
        assertThat(approvedUser.getApprovedBy().getUserId()).isEqualTo(approver.getUserId());
    }
}
