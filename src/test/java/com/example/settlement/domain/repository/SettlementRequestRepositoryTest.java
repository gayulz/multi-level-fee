package com.example.settlement.domain.repository;

import com.example.settlement.domain.entity.Organization;
import com.example.settlement.domain.entity.SettlementRequest;
import com.example.settlement.domain.entity.User;
import com.example.settlement.domain.entity.enums.SettlementStatus;
import com.example.settlement.messaging.SettlementMessageConsumer;
import com.example.settlement.messaging.SettlementMessageProducer;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [NEW] 정산 요청 Repository 통합 테스트.
 *
 * H2 인메모리 DB를 사용하여 Repository의 기능을 테스트합니다.
 *
 * @author gayul.kim
 * @since 2026-03-28
 */
@SpringBootTest
@Transactional
@ActiveProfiles("test")
class SettlementRequestRepositoryTest {

    @MockBean
    private RabbitTemplate rabbitTemplate;
    
    @MockBean
    private SettlementMessageConsumer settlementMessageConsumer;
    
    @MockBean
    private SettlementMessageProducer settlementMessageProducer;

    @Autowired
    private SettlementRequestRepository requestRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository orgRepository;

    @Autowired
    private EntityManager em;

    private Organization agencyOrg;
    private User requesterUser;

    @BeforeEach
    void setUp() {
        // 공용 테스트 픽스처 로딩: 조직 -> 사용자 -> 요청자 순서 세팅
        Organization hq = Organization.createHeadquarters("테스트 본사", "HQ-001");
        Organization branch = Organization.createBranch("테스트 지사", "BR-001", hq);
        agencyOrg = Organization.createAgency("테스트 대리점", "AG-001", branch);
        
        orgRepository.save(hq); // Cascade 적용으로 자식 노드까지 영속화됨

        requesterUser = User.createUser("requester@email.com", "pw", "대리점유저", "010-1234-5678", agencyOrg, true);
        userRepository.save(requesterUser);
    }

    @Test
    @DisplayName("정산 요청이 정상적으로 생성되고 데이터베이스에 반영되어야 함")
    void 정산요청_데이터_저장() {
        // Given
        SettlementRequest request = SettlementRequest.create(
            "ORDER-001", new BigDecimal("50000.00"), "상품 판매 정산", requesterUser, agencyOrg);
        
        // When
        SettlementRequest savedRequest = requestRepository.save(request);

        em.flush();
        em.clear();

        // Then
        Optional<SettlementRequest> found = requestRepository.findById(savedRequest.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getOrderId()).isEqualTo("ORDER-001");
        assertThat(found.get().getStatus()).isEqualTo(SettlementStatus.PENDING);
        assertThat(found.get().getCurrentApprovalLevel()).isEqualTo(1);
    }

    @Test
    @DisplayName("Fetch 조인을 활용한 특정 사용자의 정산 요청 목록 조회가 정상 동작해야 함")
    void 특정_사용자_정산요청조회_패치조인() {
        // Given
        SettlementRequest request1 = SettlementRequest.create("ORDER-001", new BigDecimal("10000"), "비품구매", requesterUser, agencyOrg);
        SettlementRequest request2 = SettlementRequest.create("ORDER-002", new BigDecimal("20000"), "물품구매", requesterUser, agencyOrg);
        requestRepository.saveAll(List.of(request1, request2));

        em.flush();
        em.clear();

        // When
        List<SettlementRequest> requests = requestRepository.findByRequesterWithDetails(requesterUser);

        // Then
        assertThat(requests).hasSize(2)
            .extracting("orderId")
            .containsExactlyInAnyOrder("ORDER-001", "ORDER-002");
        // N+1 쿼리 발생 없이 조직 및 유저 정보가 로드되어야 함
        assertThat(requests.get(0).getRequester().getEmail()).isEqualTo("requester@email.com");
        assertThat(requests.get(0).getOrganization().getOrgCode()).isEqualTo("AG-001");
    }

    @Test
    @DisplayName("특정 상태(status)의 정산 요청만 필터링하여 조회할 수 있어야 함")
    void 정산상태기반_필터링_조회() {
        // Given
        SettlementRequest pendingReq = SettlementRequest.create("ORDER-001", new BigDecimal("1000"), "결제", requesterUser, agencyOrg);
        SettlementRequest approvedReq = SettlementRequest.create("ORDER-002", new BigDecimal("2000"), "결제", requesterUser, agencyOrg);
        
        // Admin 유저 생성 (승인자 - 대리점 관리자, 레벨 3)
        User adminUser = User.createUser("admin@email.com", "pw", "대리점관리자", "010-0000-0000", agencyOrg, true);
        userRepository.save(adminUser);

        // 승인 처리 (상태를 AGENCY_APPROVED 로 전이)
        approvedReq.approve(adminUser, "승인 합니다.");
        
        requestRepository.saveAll(List.of(pendingReq, approvedReq));

        em.flush();
        em.clear();

        // When
        List<SettlementRequest> pendingList = requestRepository.findByStatus(SettlementStatus.PENDING);
        List<SettlementRequest> approvedList = requestRepository.findByStatus(SettlementStatus.AGENCY_APPROVED);

        // Then
        assertThat(pendingList).hasSize(1);
        assertThat(pendingList.get(0).getOrderId()).isEqualTo("ORDER-001");

        assertThat(approvedList).hasSize(1);
        assertThat(approvedList.get(0).getOrderId()).isEqualTo("ORDER-002");
    }

    @Test
    @DisplayName("조직별(Organization) 정산 요청 목록 조회가 정상 동작해야 함")
    void 조직기반_정산요청_조회() {
        // Given
        SettlementRequest req1 = SettlementRequest.create("ORDER-001", new BigDecimal("3000"), "결제1", requesterUser, agencyOrg);
        requestRepository.save(req1);

        em.flush();
        em.clear();

        // When
        List<SettlementRequest> orgRequests = requestRepository.findByOrganization(agencyOrg);
        List<SettlementRequest> emptyRequests = requestRepository.findByOrganization(agencyOrg.getParent());

        // Then
        assertThat(orgRequests).hasSize(1);
        assertThat(orgRequests.get(0).getOrderId()).isEqualTo("ORDER-001");
        
        assertThat(emptyRequests).isEmpty();
    }
}
