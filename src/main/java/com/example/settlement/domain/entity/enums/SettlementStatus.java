package com.example.settlement.domain.entity.enums;

/**
 * [NEW] 정산 요청 승인 상태 Enum.
 *
 * <p>
 * 정산 요청의 다단계 승인 프로세스 상태를 나타냅니다.
 * DATABASE_DESIGN.md의 settlement_request.status CHECK 제약조건과 일치합니다.
 * </p>
 *
 * <p>
 * 상태 전이 흐름 (대리점 요청 기준):
 * PENDING → AGENCY_APPROVED → BRANCH_APPROVED → COMPLETED
 * 어느 단계에서든 → REJECTED 가능
 * </p>
 *
 * <ul>
 * <li>PENDING: 정산 요청 접수, 대리점 관리자 승인 대기</li>
 * <li>AGENCY_APPROVED: 대리점 관리자 승인 완료, 지사 관리자 승인 대기</li>
 * <li>BRANCH_APPROVED: 지사 관리자 승인 완료, 본사 관리자 승인 대기</li>
 * <li>COMPLETED: 본사 최종 승인 완료, 정산 처리 진행</li>
 * <li>REJECTED: 어느 단계에서든 반려 처리</li>
 * </ul>
 *
 * @author gayul.kim
 * @since 2026-03-07
 */
public enum SettlementStatus {

    /** 정산 요청 접수 (대리점 관리자 승인 대기) */
    PENDING,

    /** 대리점 관리자 승인 완료 (지사 관리자 승인 대기) */
    AGENCY_APPROVED,

    /** 지사 관리자 승인 완료 (본사 관리자 승인 대기) */
    BRANCH_APPROVED,

    /** 본사 최종 승인 완료 (정산 처리 완료) */
    COMPLETED,

    /** 반려 처리 (어느 단계에서든 발생 가능, 이후 승인 진행 불가) */
    REJECTED
}
