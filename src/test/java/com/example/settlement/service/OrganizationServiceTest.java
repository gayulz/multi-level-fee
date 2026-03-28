package com.example.settlement.service;

import com.example.settlement.domain.entity.Organization;
import com.example.settlement.domain.entity.enums.OrgType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [NEW] 조직 관리 서비스 통합 테스트
 *
 * @author gayul.kim
 * @since 2026-03-09
 */
@SpringBootTest
@Transactional
class OrganizationServiceTest {

    @Autowired
    private OrganizationService organizationService;

    @Test
    @DisplayName("계층형 조직 생성 - 본사, 지사, 대리점 구조 생성 및 조회 확인")
    void createOrganizationHierarchy() {
        // Given & When: 본사 생성
        Organization hq = organizationService.createOrganization("테스트 본사", OrgType.HEADQUARTERS, null);

        // When: 본사 하위에 지사 생성
        Organization branch = organizationService.createOrganization("테스트 지사", OrgType.BRANCH, hq.getOrgId());

        // When: 지사 하위에 대리점 생성
        Organization agency = organizationService.createOrganization("테스트 대리점", OrgType.AGENCY, branch.getOrgId());

        // Then: 본사의 자식 확인
        Organization retrievedHq = organizationService.getOrganization(hq.getOrgId());
        assertThat(retrievedHq.getChildren()).hasSize(1);
        assertThat(retrievedHq.getChildren().get(0).getOrgName()).isEqualTo("테스트 지사");

        // Then: 레벨 검증
        assertThat(hq.getLevel()).isEqualTo(1);
        assertThat(branch.getLevel()).isEqualTo(2);
        assertThat(agency.getLevel()).isEqualTo(3);
    }
}
