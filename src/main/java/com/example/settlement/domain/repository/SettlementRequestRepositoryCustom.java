package com.example.settlement.domain.repository;

import com.example.settlement.domain.entity.SettlementRequest;
import com.example.settlement.domain.entity.enums.SettlementStatus;

import java.util.List;

/**
 * [NEW] 정산 요청 QueryDSL Custom Repository 인터페이스.
 *
 * @author gayul.kim
 * @since 2026-03-07
 */
public interface SettlementRequestRepositoryCustom {

    /**
     * 특정 승인 단계 및 상태에 해당하는 정산 요청 목록 조회.
     *
     * <p>
     * 승인 담당자가 자신이 처리해야 할 목록을 조회할 때 사용합니다.
     * </p>
     *
     * @param approvalLevel 현재 승인 단계 (1: 대리점, 2: 지사, 3: 본사)
     * @return 해당 단계에서 대기 중인 정산 요청 목록
     */
    List<SettlementRequest> findPendingByApprovalLevel(int approvalLevel);

    /**
     * 특정 조직 및 상태의 정산 요청 목록 조회 (최신순).
     *
     * @param orgId  조직 ID
     * @param status 승인 상태
     * @return 해당 조건의 정산 요청 목록 (최신순)
     */
    List<SettlementRequest> findByOrgAndStatus(Long orgId, SettlementStatus status);

    /**
     * 여러 조직의 특정 상태 및 승인 단계 정산 요청 목록 조회.
     *
     * <p>
     * 지사 관리자가 하위 대리점들의 승인 대기 목록을 조회할 때 사용합니다.
     * </p>
     *
     * @param orgIds 조직 ID 목록
     * @param status 승인 상태
     * @param level  현재 승인 단계
     * @return 해당 조건의 정산 요청 목록
     */
    List<SettlementRequest> findByOrgIdInAndApprovalLevel(List<Long> orgIds, SettlementStatus status, int level);
}
