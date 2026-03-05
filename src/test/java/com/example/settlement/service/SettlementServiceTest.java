package com.example.settlement.service;

import com.example.settlement.domain.entity.SettlementNode;
import com.example.settlement.domain.repository.SettlementNodeRepository;
import com.example.settlement.dto.SettlementRequest;
import com.example.settlement.dto.SettlementResult;
import com.example.settlement.messaging.SettlementMessageConsumer;
import com.example.settlement.messaging.SettlementMessageProducer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [NEW] 정산 서비스 계층(비즈니스 로직) 통합 테스트.
 *
 * Javadoc @author gayul.kim
 * 
 * @since 2026-03-06
 */
@SpringBootTest
@Transactional
@ActiveProfiles("test")
class SettlementServiceTest {

    @MockBean
    private RabbitTemplate rabbitTemplate;
    @MockBean
    private SettlementMessageConsumer settlementMessageConsumer;
    @MockBean
    private SettlementMessageProducer settlementMessageProducer;
    @Autowired
    private SettlementService settlementService;

    @Autowired
    private SettlementNodeRepository repository;

    @Test
    @DisplayName("1. 단일 노드(루트) 독식 정산 및 낙전 보정 검증")
    void testSingleRootNodeSettlement() {
        // Given: 본사만 있는 경우 (수수료 10%)
        SettlementNode root = new SettlementNode("본사", new BigDecimal("0.10"));
        repository.save(root);

        SettlementRequest request = new SettlementRequest("ORDER-1", 10000L, root.getId());

        // When: 10000원 정산 실행
        SettlementResult result = settlementService.calculate(request);

        // Then
        // 원래 수수료는 1000원이지만 자식이 없으므로 남은 금액 9000원(낙전)이 모두 루트 노드로 귀속되어야 함.
        // 최종 수수료는 1000 + 9000 = 10000원.
        assertThat(result.nodeName()).isEqualTo("본사");
        assertThat(result.feeAmount()).isEqualByComparingTo(BigDecimal.valueOf(10000));
        assertThat(result.childResults()).isEmpty();
    }

    @Test
    @DisplayName("2. 다단계 트리 및 1/N 등분 분배 정산 검증 (낙전 보정 포함)")
    void testMultiLevelTreeSettlementAndDustCorrection() {
        // Given
        // 본사(10%) -> 지사A(5%), 지사B(5%) -> 각 지사 밑 대리점 2곳씩(3%)
        SettlementNode hq = new SettlementNode("본사", new BigDecimal("0.10"));

        SettlementNode branchA = new SettlementNode("지사A", new BigDecimal("0.05"));
        SettlementNode branchB = new SettlementNode("지사B", new BigDecimal("0.05"));

        SettlementNode agencyA1 = new SettlementNode("대리점A1", new BigDecimal("0.03"));
        SettlementNode agencyA2 = new SettlementNode("대리점A2", new BigDecimal("0.03"));

        SettlementNode agencyB1 = new SettlementNode("대리점B1", new BigDecimal("0.03"));
        SettlementNode agencyB2 = new SettlementNode("대리점B2", new BigDecimal("0.03"));

        hq.addChild(branchA);
        hq.addChild(branchB);

        branchA.addChild(agencyA1);
        branchA.addChild(agencyA2);

        branchB.addChild(agencyB1);
        branchB.addChild(agencyB2);

        repository.save(hq);

        // 계층도 저장 후 요청
        SettlementRequest request = new SettlementRequest("ORDER-2", 10000L, hq.getId());

        // When
        SettlementResult result = settlementService.calculate(request);

        // Then
        // [검증 시나리오]
        // 1. 본사: 10000원 * 10% = 1000원 수익 (9000원 남음)
        // 2. 지사A/B (1/N 등분): 9000 / 2 = 4500원씩 받음
        // - 지사A 수익: 4500원 * 5% = 225원 (4275원 남음)
        // - 지사B 수익: 4500원 * 5% = 225원 (4275원 남음)
        // 3. 대리점 (1/N 등분): 각 대리점은 부모(지사)가 남긴 4275원을 / 2 = 2137원씩 받음 (소수점 버림 방식을 감안)
        // - 대리점A1 수익: 2137원 * 3% = 64원
        // - 대리점A2 수익: 2137원 * 3% = 64원
        // - 대리점B1 수익: 2137원 * 3% = 64원
        // - 대리점B2 수익: 2137원 * 3% = 64원
        // 4. 총 수수료 합계: 1000(본사) + 450(지사) + 256(대리점) = 1706원
        // 5. 낙전(Dust): 10000(원금) - 1706 = 8294원
        // 6. 결과적으로 본사 수익은 "기본 할당 1000원 + 낙전 8294원 = 9294원" 이어야 한다!

        assertThat(result.feeAmount()).isEqualByComparingTo(BigDecimal.valueOf(9294));
        assertThat(result.childResults()).hasSize(2);

        SettlementResult resultBranchA = result.childResults().stream()
                .filter(r -> r.nodeName().equals("지사A")).findFirst().orElseThrow();
        assertThat(resultBranchA.feeAmount()).isEqualByComparingTo(BigDecimal.valueOf(225));

        SettlementResult resultAgencyA1 = resultBranchA.childResults().stream()
                .filter(r -> r.nodeName().equals("대리점A1")).findFirst().orElseThrow();
        assertThat(resultAgencyA1.feeAmount()).isEqualByComparingTo(BigDecimal.valueOf(64));
    }
}
