package com.example.settlement.service;

import com.example.settlement.domain.entity.Organization;
import com.example.settlement.domain.entity.SettlementNode;
import com.example.settlement.domain.entity.SettlementRequest;
import com.example.settlement.domain.entity.User;
import com.example.settlement.domain.entity.enums.OrgType;
import com.example.settlement.domain.entity.enums.SettlementStatus;
import com.example.settlement.domain.entity.enums.UserRole;
import com.example.settlement.dto.NodeCreateRequest;
import com.example.settlement.dto.request.SettlementRequestDto;
import com.example.settlement.dto.request.SignupRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ApprovalProcessIntegrationTest {

    @Autowired
    SettlementService settlementService;

    @Autowired
    ApprovalService approvalService;

    @Autowired
    UserService userService;

    @Autowired
    OrganizationService organizationService;

    private User createAdmin(String email, Long orgId) {
        SignupRequest req = new SignupRequest();
        req.setEmail(email);
        req.setPassword("password");
        req.setName("Admin");
        req.setPhone("010-0000-0000");
        req.setOrganizationId(orgId);
        User user = userService.registerUser(req);
        user.verifyEmail();
        userService.changeUserRole(user.getUserId(), UserRole.ROLE_ADMIN);
        return user;
    }

    private User createUser(String email, Long orgId) {
        SignupRequest req = new SignupRequest();
        req.setEmail(email);
        req.setPassword("password");
        req.setName("User");
        req.setPhone("010-0000-0000");
        req.setOrganizationId(orgId);
        User user = userService.registerUser(req);
        user.verifyEmail();
        return user;
    }

    @Test
    @DisplayName("대리점 사용자 정산 요청 다단계 승인 성공 과정")
    void 대리점_사용자_정산_요청_다단계_승인() {
        // Given: 본사 -> 지사 -> 대리점 조직 구조 생성
        Organization hq = organizationService.createOrganization("본사", OrgType.HEADQUARTERS, null);
        Organization branch = organizationService.createOrganization("지사", OrgType.BRANCH, hq.getOrgId());
        Organization agency = organizationService.createOrganization("대리점", OrgType.AGENCY, branch.getOrgId());

        // 각 조직의 SettlementNode 생성
        SettlementNode hqNode = settlementService
                .createNode(new NodeCreateRequest("본사 노드", new BigDecimal("0.02"), null));
        SettlementNode branchNode = settlementService
                .createNode(new NodeCreateRequest("지사 노드", new BigDecimal("0.05"), hqNode.getId()));
        SettlementNode agencyNode = settlementService
                .createNode(new NodeCreateRequest("대리점 노드", new BigDecimal("0.10"), branchNode.getId()));

        // TODO: Organization과 SettlementNode 연관관계 설정 필요
        // 현재 OrganizationService/SettlementService에 연관관계 연결 로직이 명확히 구현되지 않았다면 수동 설정
        // 이 예제에서는 DTO에 rootNodeId가 포함되므로 agencyNode를 전달

        // 각 조직의 관리자 및 대리점 일반 사용자 생성
        User hqAdmin = createAdmin("hq@test.com", hq.getOrgId());
        User branchAdmin = createAdmin("branch@test.com", branch.getOrgId());
        User agencyAdmin = createAdmin("agency@test.com", agency.getOrgId());
        User agencyUser = createUser("user@test.com", agency.getOrgId());

        // When (1): 일반 사용자의 정산 요청 접수
        SettlementRequestDto requestDto = new SettlementRequestDto(
                "ORD-2026-03-09-001",
                new BigDecimal("10000"),
                agencyNode.getId(),
                "테스트 결제 정산");
        SettlementRequest request = settlementService.createRequest(requestDto, agencyUser);

        // Then (1): 대리점 승인 대기 상태
        assertThat(request.getStatus()).isEqualTo(SettlementStatus.PENDING);
        assertThat(request.getCurrentApprovalLevel()).isEqualTo(1);

        // When (2): 대리점 관리자 승인
        approvalService.approve(request.getId(), agencyAdmin, "대리점 검토 완료");
        request = settlementService.getRequest(request.getId()); // 영속성 컨텍스트 초기화 안했다면 상태 갱신 필요

        // Then (2): 지사 승인 대기 상태
        assertThat(request.getStatus()).isEqualTo(SettlementStatus.AGENCY_APPROVED);
        assertThat(request.getCurrentApprovalLevel()).isEqualTo(2);

        // When (3): 지사 관리자 승인
        approvalService.approve(request.getId(), branchAdmin, "지사 검토 완료");
        request = settlementService.getRequest(request.getId());

        // Then (3): 본사(최종) 승인 대기 상태
        assertThat(request.getStatus()).isEqualTo(SettlementStatus.BRANCH_APPROVED);
        assertThat(request.getCurrentApprovalLevel()).isEqualTo(3);

        // When (4): 본사 관리자 최종 승인
        approvalService.approve(request.getId(), hqAdmin, "최종 승인");
        request = settlementService.getRequest(request.getId());

        // Then (4): 처리 완료 상태
        assertThat(request.getStatus()).isEqualTo(SettlementStatus.COMPLETED);
    }
}
