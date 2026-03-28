package com.example.settlement.domain.repository;

import com.example.settlement.domain.entity.Organization;
import com.example.settlement.domain.entity.enums.OrgType;
import com.example.settlement.messaging.SettlementMessageConsumer;
import com.example.settlement.messaging.SettlementMessageProducer;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [NEW] 조직 Repository 통합 테스트.
 *
 * H2 인메모리 DB를 사용하여 Repository의 기능을 테스트합니다.
 *
 * @author gayul.kim
 * @since 2026-03-28
 */
@SpringBootTest
@Transactional
@ActiveProfiles("test")
class OrganizationRepositoryTest {

    @MockBean
    private RabbitTemplate rabbitTemplate;
    
    @MockBean
    private SettlementMessageConsumer settlementMessageConsumer;
    
    @MockBean
    private SettlementMessageProducer settlementMessageProducer;

    @Autowired
    private OrganizationRepository repository;

    @Autowired
    private EntityManager em;

    @Test
    @DisplayName("조직 계층 구조(본사-지사-대리점)를 정상적으로 저장하고 조회할 수 있어야 함")
    void 조직_계층구조_저장_조회() {
        // Given
        Organization hq = Organization.createHeadquarters("본사", "HQ-001");
        Organization branch = Organization.createBranch("지사", "BR-001", hq);
        Organization agency = Organization.createAgency("대리점", "AG-001", branch);

        repository.save(hq); // Cascade.ALL 에 의해 자식들도 저장됨

        em.flush();
        em.clear();

        // When
        Organization foundHq = repository.findByIdWithChildren(hq.getOrgId()).orElseThrow();

        // Then
        assertThat(foundHq.getOrgName()).isEqualTo("본사");
        assertThat(foundHq.getChildren()).hasSize(1);
        
        Organization foundBranch = foundHq.getChildren().get(0);
        assertThat(foundBranch.getOrgName()).isEqualTo("지사");
        assertThat(foundBranch.getChildren()).hasSize(1);

        Organization foundAgency = foundBranch.getChildren().get(0);
        assertThat(foundAgency.getOrgName()).isEqualTo("대리점");
        assertThat(foundAgency.getLevel()).isEqualTo(3);
        assertThat(foundAgency.getParent().getOrgCode()).isEqualTo("BR-001");
    }

    @Test
    @DisplayName("최상위 본사 조직 목록만 조회할 수 있어야 함")
    void 본사_조직_조회() {
        // Given
        Organization hq1 = Organization.createHeadquarters("본사1", "HQ-001");
        Organization branch1 = Organization.createBranch("지사1", "BR-001", hq1);
        
        Organization hq2 = Organization.createHeadquarters("본사2", "HQ-002");

        repository.saveAll(List.of(hq1, hq2));

        em.flush();
        em.clear();

        // When
        List<Organization> rootOrgs = repository.findByParentIsNull();

        // Then
        assertThat(rootOrgs).hasSize(2)
            .extracting("orgName")
            .containsExactlyInAnyOrder("본사1", "본사2");
        assertThat(rootOrgs.get(0).getLevel()).isEqualTo(1);
    }

    @Test
    @DisplayName("조직 코드를 이용해 조직을 단건 조회할 수 있어야 함")
    void 조직_코드별_조회() {
        // Given
        Organization hq = Organization.createHeadquarters("본사", "HQ-999");
        repository.save(hq);

        em.flush();
        em.clear();

        // When
        Optional<Organization> found = repository.findByOrgCode("HQ-999");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getOrgName()).isEqualTo("본사");
    }

    @Test
    @DisplayName("특정 조직 ID를 기준으로 모든 하위 조직(직계 및 손자 포함)을 조회할 수 있어야 함")
    void 하위_조직_모든계층_조회() {
        // Given
        Organization hq = Organization.createHeadquarters("본사", "HQ-001");
        Organization branch1 = Organization.createBranch("지사1", "BR-001", hq);
        Organization branch2 = Organization.createBranch("지사2", "BR-002", hq);
        Organization agency1 = Organization.createAgency("대리점1", "AG-001", branch1);
        Organization agency2 = Organization.createAgency("대리점2", "AG-002", branch1);

        repository.save(hq);

        em.flush();
        em.clear();

        // When
        List<Organization> descendants = repository.findDescendants(hq.getOrgId());

        // Then
        assertThat(descendants).hasSize(4)
            .extracting("orgCode")
            .containsExactlyInAnyOrder("BR-001", "BR-002", "AG-001", "AG-002");
    }
}
