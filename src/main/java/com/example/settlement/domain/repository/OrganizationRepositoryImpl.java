package com.example.settlement.domain.repository;

import com.example.settlement.domain.entity.Organization;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;

import static com.example.settlement.domain.entity.QOrganization.organization;

/**
 * [NEW] 조직 QueryDSL Custom Repository 구현체.
 *
 * <p>
 * 기존 SettlementNodeRepositoryImpl 패턴을 동일하게 따릅니다.
 * JPAQueryFactory + static import Q클래스 방식으로 구현합니다.
 * </p>
 *
 * @author gayul.kim
 * @since 2026-03-07
 */
@Repository
@RequiredArgsConstructor
public class OrganizationRepositoryImpl implements OrganizationRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    /**
     * {@inheritDoc}
     *
     * @author gayul.kim
     */
    @Override
    public Optional<Organization> findByIdWithChildren(Long orgId) {
        Organization result = queryFactory
                .selectFrom(organization)
                .leftJoin(organization.children).fetchJoin()
                .where(organization.orgId.eq(orgId))
                .fetchOne();

        return Optional.ofNullable(result);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * distinct()를 사용하여 fetchJoin의 중복 결과를 제거합니다.
     * </p>
     *
     * @author gayul.kim
     */
    @Override
    public List<Organization> findAllWithChildrenFetch() {
        return queryFactory
                .selectFrom(organization)
                .distinct()
                .leftJoin(organization.children).fetchJoin()
                .orderBy(organization.level.asc(), organization.orgId.asc())
                .fetch();
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * BFS(너비 우선 탐색)로 하위 조직을 순차적으로 수집합니다.
     * 재귀 대신 Queue를 사용하여 스택 오버플로를 방지합니다.
     * </p>
     *
     * @author gayul.kim
     */
    @Override
    public List<Organization> findDescendants(Long orgId) {
        List<Organization> descendants = new ArrayList<>();
        Queue<Long> queue = new ArrayDeque<>();

        // 직계 자식 조직 조회 후 큐에 추가
        List<Organization> directChildren = queryFactory
                .selectFrom(organization)
                .where(organization.parent.orgId.eq(orgId))
                .fetch();

        for (Organization child : directChildren) {
            descendants.add(child);
            queue.add(child.getOrgId());
        }

        // BFS로 모든 자손 조직 수집
        while (!queue.isEmpty()) {
            Long parentId = queue.poll();
            List<Organization> children = queryFactory
                    .selectFrom(organization)
                    .where(organization.parent.orgId.eq(parentId))
                    .fetch();

            for (Organization child : children) {
                descendants.add(child);
                queue.add(child.getOrgId());
            }
        }

        return descendants;
    }
}
