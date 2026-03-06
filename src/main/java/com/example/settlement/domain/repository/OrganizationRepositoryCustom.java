package com.example.settlement.domain.repository;

import com.example.settlement.domain.entity.Organization;

import java.util.List;
import java.util.Optional;

/**
 * [NEW] 조직 QueryDSL Custom Repository 인터페이스.
 *
 * <p>
 * JPA 기본 메서드로 처리 불가한 복잡한 쿼리를 정의합니다.
 * </p>
 *
 * @author gayul.kim
 * @since 2026-03-07
 */
public interface OrganizationRepositoryCustom {

    /**
     * ID로 조직 조회 (자식 조직 fetchJoin 포함).
     *
     * <p>
     * N+1 방지를 위해 자식 조직을 함께 로딩합니다.
     * </p>
     *
     * @param orgId 조직 ID
     * @return 자식 조직이 포함된 조직
     */
    Optional<Organization> findByIdWithChildren(Long orgId);

    /**
     * 전체 조직 목록 조회 (자식 조직 fetchJoin 포함).
     *
     * <p>
     * 트리 전체를 한 번의 쿼리로 로딩합니다.
     * </p>
     *
     * @return 자식 조직이 포함된 전체 조직 목록
     */
    List<Organization> findAllWithChildrenFetch();

    /**
     * 특정 조직의 하위 조직 전체 조회.
     *
     * <p>
     * 직계 자식뿐 아니라 모든 자손 조직을 반환합니다.
     * 애플리케이션 레벨 BFS로 구현합니다.
     * </p>
     *
     * @param orgId 기준 조직 ID
     * @return 하위 조직 목록 (기준 조직 제외)
     */
    List<Organization> findDescendants(Long orgId);
}
