package com.example.settlement.domain.repository;

import com.example.settlement.domain.entity.SettlementNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [NEW] 정산 노드 Repository 통합 테스트.
 *
 * H2 인메모리 DB를 사용하여 Repository의 기능을 테스트합니다.
 * application-test.yml 설정(@ActiveProfiles)을 적용받아 RabbitMQ 연결 등을 제외합니다.
 *
 * @author gayul.kim
 * @since 2026-03-06
 */
@SpringBootTest
@Transactional
@ActiveProfiles("test")
class SettlementNodeRepositoryTest {

    @Autowired
    private SettlementNodeRepository repository;

    @Test
    @DisplayName("루트 노드를 저장하고 ID로 조회할 수 있어야 함")
    void 루트_노드_저장_및_조회() {
        // Given
        SettlementNode root = new SettlementNode("본사", new BigDecimal("0.10"));
        repository.save(root);

        // When
        Optional<SettlementNode> found = repository.findById(root.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("본사");
        assertThat(found.get().getFeeRate()).isEqualByComparingTo(new BigDecimal("0.10"));
    }

    @Test
    @DisplayName("부모와 자식 관계를 맺고 자식을 조회할 수 있어야 함")
    void 부모_자식_관계_설정_및_조회() {
        // Given
        SettlementNode root = new SettlementNode("본사", new BigDecimal("0.10"));
        SettlementNode child = new SettlementNode("지사", new BigDecimal("0.05"));

        root.addChild(child); // 연관관계 편의 메서드 호출
        repository.save(root); // CascadeType.ALL에 의해 자식까지 함께 저장됨

        // When
        // findByIdWithChildren은 커스텀 QueryDSL 메서드로 fetch join을 수행
        SettlementNode foundRoot = repository.findByIdWithChildren(root.getId()).orElseThrow();

        // Then
        assertThat(foundRoot.getChildren()).hasSize(1);
        assertThat(foundRoot.getChildren().get(0).getName()).isEqualTo("지사");
        assertThat(foundRoot.getChildren().get(0).getParent()).isEqualTo(foundRoot); // 양방향 매핑 확인
    }

    @Test
    @DisplayName("3단계 트리(본사-지사-대리점)를 정상적으로 구성할 수 있어야 함")
    void 트리_3단계_구조_검증() {
        // Given
        SettlementNode root = new SettlementNode("본사", new BigDecimal("0.10"));
        SettlementNode branch = new SettlementNode("지사", new BigDecimal("0.05"));
        SettlementNode agency = new SettlementNode("대리점", new BigDecimal("0.03"));

        root.addChild(branch);
        branch.addChild(agency);

        repository.save(root);

        // When
        SettlementNode foundRoot = repository.findByIdWithChildren(root.getId()).orElseThrow();
        SettlementNode foundBranch = repository.findByIdWithChildren(branch.getId()).orElseThrow();

        // Then
        assertThat(foundRoot.getChildren()).hasSize(1);
        assertThat(foundBranch.getChildren()).hasSize(1);
        assertThat(foundBranch.getChildren().get(0).getName()).isEqualTo("대리점");
    }

    @Test
    @DisplayName("findAllRootNodes 호출 시 부모가 없는 최상위 노드만 조회되어야 함")
    void 루트_노드_목록_조회_확인() {
        // Given
        SettlementNode root1 = new SettlementNode("서울본사", new BigDecimal("0.10"));
        SettlementNode child1 = new SettlementNode("강남지사", new BigDecimal("0.05"));
        root1.addChild(child1);

        SettlementNode root2 = new SettlementNode("부산본사", new BigDecimal("0.10"));

        repository.save(root1);
        repository.save(root2);

        // When
        List<SettlementNode> rootNodes = repository.findAllRootNodes();

        // Then
        assertThat(rootNodes).hasSize(2);
        assertThat(rootNodes).extracting("name").containsExactlyInAnyOrder("서울본사", "부산본사");
    }

    @Test
    @DisplayName("정산 금액의 수수료 계산 로직 결과가 정확해야 함 (소수점 버림)")
    void 수수료_계산_검증() {
        // Given
        // 수수료율 3% (0.0300)
        SettlementNode node = new SettlementNode("테스트노드", new BigDecimal("0.0300"));
        BigDecimal amount = new BigDecimal("10000"); // 10,000원

        // When
        BigDecimal fee = node.calculateFee(amount);

        // Then
        assertThat(fee).isEqualByComparingTo(new BigDecimal("300")); // 10000 * 0.03 = 300
    }

    @Test
    @DisplayName("노드가 루트인지, 리프인지 상태 판별이 정확해야 함")
    void 루트_리프_상태_검증() {
        // Given
        SettlementNode root = new SettlementNode("루트", new BigDecimal("0.10"));
        SettlementNode middle = new SettlementNode("중간", new BigDecimal("0.05"));
        SettlementNode leaf = new SettlementNode("리프", new BigDecimal("0.01"));

        root.addChild(middle);
        middle.addChild(leaf);

        // Then
        assertThat(root.isRoot()).isTrue();
        assertThat(root.isLeaf()).isFalse();

        assertThat(middle.isRoot()).isFalse();
        assertThat(middle.isLeaf()).isFalse();

        assertThat(leaf.isRoot()).isFalse();
        assertThat(leaf.isLeaf()).isTrue();
    }
}
