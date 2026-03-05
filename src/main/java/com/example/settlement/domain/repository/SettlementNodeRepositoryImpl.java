package com.example.settlement.domain.repository;

import com.example.settlement.domain.entity.SettlementNode;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static com.example.settlement.domain.entity.QSettlementNode.settlementNode;

/**
 * [NEW] 정산 노드 Custom Repository 구현체.
 *
 * QueryDSL을 사용하여 동적 쿼리 및 페치 조인을 구현합니다.
 *
 * @author gayul.kim
 * @since 2026-03-06
 */
@Repository
@RequiredArgsConstructor
public class SettlementNodeRepositoryImpl implements SettlementNodeRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<SettlementNode> findByIdWithChildren(Long id) {
        SettlementNode result = queryFactory
                .selectFrom(settlementNode)
                .leftJoin(settlementNode.children).fetchJoin()
                .where(settlementNode.id.eq(id))
                .fetchOne();

        return Optional.ofNullable(result);
    }

    @Override
    public List<SettlementNode> findAllRootNodes() {
        return queryFactory
                .selectFrom(settlementNode)
                .where(settlementNode.parent.isNull())
                .fetch();
    }
}
