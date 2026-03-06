package com.example.settlement.domain.repository;

import com.example.settlement.domain.entity.Organization;
import com.example.settlement.domain.entity.SettlementRequest;
import com.example.settlement.domain.entity.User;
import com.example.settlement.domain.entity.enums.SettlementStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * [NEW] 정산 요청 JPA Repository.
 *
 * @author gayul.kim
 * @since 2026-03-07
 */
public interface SettlementRequestRepository
        extends JpaRepository<SettlementRequest, Long>, SettlementRequestRepositoryCustom {

    /**
     * 요청자별 정산 요청 목록 조회.
     *
     * @param requester 요청자
     * @return 해당 요청자의 정산 요청 목록
     */
    List<SettlementRequest> findByRequester(User requester);

    /**
     * 요청자별 정산 요청 목록 조회 (최신순).
     *
     * @param requester 요청자
     * @return 해당 요청자의 정산 요청 목록 (최신순)
     */
    List<SettlementRequest> findByRequesterOrderByCreatedAtDesc(User requester);

    /**
     * 승인 상태별 정산 요청 목록 조회.
     *
     * @param status 승인 상태
     * @return 해당 상태의 정산 요청 목록
     */
    List<SettlementRequest> findByStatus(SettlementStatus status);

    /**
     * 조직별 정산 요청 목록 조회.
     *
     * @param organization 조직
     * @return 해당 조직의 정산 요청 목록
     */
    List<SettlementRequest> findByOrganization(Organization organization);

    /**
     * 주문 ID로 정산 요청 존재 여부 확인 (중복 요청 방지).
     *
     * @param orderId 주문 ID
     * @return 존재하면 true
     */
    boolean existsByOrderId(String orderId);
}
