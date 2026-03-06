package com.example.settlement.domain.entity;

import com.example.settlement.domain.entity.enums.OrgType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * [NEW] 조직 Entity.
 *
 * <p>
 * 본사-지사-대리점의 3단계 계층 구조를 Self-Reference Tree로 표현합니다.
 * level 필드를 DB에 직접 저장하여 재귀 조회 없이 계층 판별이 가능합니다.
 * </p>
 *
 * <p>
 * 계층 구조:
 * level=1: 본사(HEADQUARTERS), parent=null
 * level=2: 지사(BRANCH), parent=본사
 * level=3: 대리점(AGENCY), parent=지사
 * </p>
 *
 * @author gayul.kim
 * @since 2026-03-07
 */
@Entity
@Table(name = "organization")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "org_id")
    private Long orgId;

    @Column(name = "org_name", nullable = false, length = 100)
    private String orgName;

    @Enumerated(EnumType.STRING)
    @Column(name = "org_type", nullable = false, length = 20)
    private OrgType orgType;

    @Column(name = "org_code", nullable = false, unique = true, length = 50)
    private String orgCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_org_id")
    private Organization parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    private List<Organization> children = new ArrayList<>();

    @Column(nullable = false)
    private Integer level;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // =========================================================
    // 정적 팩토리 메서드
    // =========================================================

    /**
     * [NEW] 본사 조직 생성.
     *
     * @param orgName 조직명
     * @param orgCode 조직 코드 (예: HQ-001)
     * @return 본사 Organization (level=1, parent=null)
     * @author gayul.kim
     */
    public static Organization createHeadquarters(String orgName, String orgCode) {
        Organization org = new Organization();
        org.orgName = orgName;
        org.orgCode = orgCode;
        org.orgType = OrgType.HEADQUARTERS;
        org.level = 1;
        org.active = true;
        return org;
    }

    /**
     * [NEW] 지사 조직 생성.
     *
     * @param orgName 조직명
     * @param orgCode 조직 코드 (예: BR-001)
     * @param parent  상위 본사 Organization
     * @return 지사 Organization (level=2)
     * @author gayul.kim
     */
    public static Organization createBranch(String orgName, String orgCode, Organization parent) {
        Organization org = new Organization();
        org.orgName = orgName;
        org.orgCode = orgCode;
        org.orgType = OrgType.BRANCH;
        org.level = 2;
        org.active = true;
        parent.addChild(org);
        return org;
    }

    /**
     * [NEW] 대리점 조직 생성.
     *
     * @param orgName 조직명
     * @param orgCode 조직 코드 (예: AG-001)
     * @param parent  상위 지사 Organization
     * @return 대리점 Organization (level=3)
     * @author gayul.kim
     */
    public static Organization createAgency(String orgName, String orgCode, Organization parent) {
        Organization org = new Organization();
        org.orgName = orgName;
        org.orgCode = orgCode;
        org.orgType = OrgType.AGENCY;
        org.level = 3;
        org.active = true;
        parent.addChild(org);
        return org;
    }

    // =========================================================
    // 연관관계 편의 메서드
    // =========================================================

    /**
     * [NEW] 자식 조직 추가 (양방향 연관관계 설정).
     *
     * @param child 추가할 자식 조직
     * @author gayul.kim
     */
    public void addChild(Organization child) {
        this.children.add(child);
        child.parent = this;
    }

    // =========================================================
    // 비즈니스 메서드
    // =========================================================

    /**
     * [NEW] 루트 조직(본사) 여부 확인.
     *
     * @return parent가 null이면 true (본사)
     * @author gayul.kim
     */
    public boolean isRoot() {
        return this.parent == null;
    }

    /**
     * [NEW] 말단 조직(대리점) 여부 확인.
     *
     * @return 자식 조직이 없으면 true
     * @author gayul.kim
     */
    public boolean isLeaf() {
        return this.children.isEmpty();
    }

    /**
     * [NEW] 현재 조직의 모든 상위 조직 목록 반환 (루트까지).
     *
     * <p>
     * 순서: 직계 부모부터 루트(본사) 순으로 반환됩니다.
     * </p>
     *
     * @return 상위 조직 목록
     * @author gayul.kim
     */
    public List<Organization> getAncestors() {
        List<Organization> ancestors = new ArrayList<>();
        Organization current = this.parent;
        while (current != null) {
            ancestors.add(current);
            current = current.getParent();
        }
        return ancestors;
    }

    /**
     * [NEW] 현재 조직의 모든 하위 조직 목록 반환 (재귀).
     *
     * <p>
     * 주의: 대용량 트리에서는 N+1 이슈 발생 가능.
     * 필요 시 Repository에서 fetchJoin 또는 재귀 CTE 쿼리 사용 권장.
     * </p>
     *
     * @return 하위 조직 목록 (자식 + 자손 전체)
     * @author gayul.kim
     */
    public List<Organization> getAllDescendants() {
        List<Organization> descendants = new ArrayList<>();
        for (Organization child : this.children) {
            descendants.add(child);
            descendants.addAll(child.getAllDescendants());
        }
        return descendants;
    }

    /**
     * [NEW] 조직 비활성화 처리.
     *
     * <p>
     * 비활성화 시 소속 사용자는 로그인 불가.
     * </p>
     *
     * @author gayul.kim
     */
    public void deactivate() {
        this.active = false;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * [NEW] 조직명 수정.
     *
     * @param orgName 새로운 조직명
     * @author gayul.kim
     */
    public void updateOrgName(String orgName) {
        this.orgName = orgName;
        this.updatedAt = LocalDateTime.now();
    }

    // =========================================================
    // 생명주기 콜백
    // =========================================================

    @PrePersist
    private void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    private void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
