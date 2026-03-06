package com.example.settlement.domain.repository;

import com.example.settlement.domain.entity.SettlementRequest;
import com.example.settlement.domain.entity.enums.SettlementStatus;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.example.settlement.domain.entity.QSettlementRequest.settlementRequest;

/**
 * [NEW] 정산 요청 QueryDSL Custom Repository 구현체.
 *
 * @author gayul.kim
 * @since 2026-03-07
 */
@Repository
@RequiredArgsConstructor
public class SettlementRequestRepositoryImpl implements SettlementRequestRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    /**
     * {@inheritDoc}
     *
     * <p>
     * REJECTED 상태는 제외하고 현재 승인 단계가 일치하는 요청만 반환합니다.
     * </p>
     *
     * @author gayul.kim
     */
    @Override
    public List<SettlementRequest> findPendingByApprovalLevel(int approvalLevel) {
        return queryFactory
                .selectFrom(settlementRequest)
                .where(
                        settlementRequest.currentApprovalLevel.eq(approvalLevel),
                        settlementRequest.status.ne(SettlementStatus.REJECTED),
                        settlementRequest.status.ne(SettlementStatus.COMPLETED))
                .orderBy(settlementRequest.createdAt.asc())
                .fetch();
    }

    /**
     * {@inheritDoc}
     *
     * @author gayul.kim
     */
    @Override
    public List<SettlementRequest> findByOrgAndStatus(Long orgId, SettlementStatus status) {
        return queryFactory
                .selectFrom(settlementRequest)
                .where(
                        settlementRequest.organization.orgId.eq(orgId),
                        settlementRequest.status.eq(status))
                .orderBy(settlementRequest.createdAt.desc())
                .fetch();
    }

    /**
     * {@inheritDoc}
     *
     * @author gayul.kim
     */
    @Override
    public List<SettlementRequest> findByOrgIdInAndApprovalLevel(
            List<Long> orgIds,
            SettlementStatus status,
            int level) {
        return queryFactory
                .selectFrom(settlementRequest)
                .where(
                        settlementRequest.organization.orgId.in(orgIds),
                        settlementRequest.status.eq(status),
                        settlementRequest.currentApprovalLevel.eq(level))
                .orderBy(settlementRequest.createdAt.asc())
                .fetch();
    }
}
