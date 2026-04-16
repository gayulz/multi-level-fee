package com.example.settlement.domain.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * [NEW] 정산 노드 Entity.
 *
 * Self-Reference Tree 구조로 다단계 정산 트리를 표현합니다.
 * 각 노드는 부모 노드(parent)와 자식 노드 목록(children)을 가지며,
 * 수수료율(feeRate)에 따라 정산 금액을 계산합니다.
 *
 * @author gayul.kim
 * @since 2026-03-06
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SettlementNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal feeRate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private SettlementNode parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    private List<SettlementNode> children = new ArrayList<>();

    /**
     * Organization과 1:1 매핑.
     * nullable=true: 기존 테스트 데이터와의 호환성 유지.
     * Organization 삭제 시 SettlementNode도 함께 삭제됩니다 (CASCADE).
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", unique = true)
    private Organization organization;

    /**
     * [NEW] 정산 노드 생성자.
     *
     * @param name    노드 이름
     * @param feeRate 수수료율
     */
    public SettlementNode(String name, BigDecimal feeRate) {
        this.name = name;
        this.feeRate = feeRate;
    }

    /**
     * [NEW] 수수료 계산.
     *
     * 거래 금액에 수수료율을 곱하여 수수료를 계산합니다.
     * 소수점 이하는 버림(FLOOR) 처리합니다.
     *
     * @param amount 거래 금액
     * @return 수수료 금액 (소수점 버림)
     */
    public BigDecimal calculateFee(BigDecimal amount) {
        return amount.multiply(feeRate).setScale(0, RoundingMode.FLOOR);
    }

    /**
     * [NEW] 자식 노드 추가.
     *
     * 양방향 연관관계를 설정합니다.
     *
     * @param child 추가할 자식 노드
     */
    public void addChild(SettlementNode child) {
        this.children.add(child);
        child.parent = this;
    }

    /**
     * [NEW] 노드 정보 수정.
     */
    public void update(String name, BigDecimal feeRate) {
        this.name = name;
        this.feeRate = feeRate;
    }

    /**
     * [NEW] 상위 노드 변경.
     */
    public void changeParent(SettlementNode newParent) {
        if (this.parent != null) {
            this.parent.getChildren().remove(this);
        }
        this.parent = newParent;
        if (newParent != null) {
            newParent.getChildren().add(this);
        }
    }

    /**
     * [NEW] 루트 노드 여부 확인.
     *
     * @return 부모가 없으면 true
     */
    public boolean isRoot() {
        return this.parent == null;
    }

    /**
     * [NEW] 리프 노드 여부 확인.
     *
     * @return 자식이 없으면 true
     */
    /**
     * [NEW] 리프 노드 여부 확인.
     *
     * @return 자식이 없으면 true
     */
    public boolean isLeaf() {
        return this.children.isEmpty();
    }

    /**
     * [NEW] 루트 정산 노드 생성 (부모 없음).
     *
     * @param name         노드명
     * @param organization 소속 조직
     * @param feeRate      수수료율
     * @return 루트 SettlementNode
     * @author gayul.kim
     */
    public static SettlementNode createRoot(String name, Organization organization, BigDecimal feeRate) {
        SettlementNode node = new SettlementNode();
        node.name = name;
        node.feeRate = feeRate;
        node.organization = organization;
        return node;
    }

    /**
     * [NEW] 자식 정산 노드 생성 (양방향 연관관계 설정 포함).
     *
     * @param name         노드명
     * @param organization 소속 조직
     * @param feeRate      수수료율
     * @param parent       부모 노드
     * @return 자식 SettlementNode
     * @author gayul.kim
     */
    public static SettlementNode createChild(String name, Organization organization,
            BigDecimal feeRate, SettlementNode parent) {
        SettlementNode node = new SettlementNode();
        node.name = name;
        node.feeRate = feeRate;
        node.organization = organization;
        parent.addChild(node);
        return node;
    }
}
