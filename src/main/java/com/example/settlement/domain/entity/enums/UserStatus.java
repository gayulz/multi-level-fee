package com.example.settlement.domain.entity.enums;

/**
 * [NEW] 사용자 가입 상태 Enum.
 *
 * <p>
 * 사용자의 가입 승인 프로세스 상태를 나타냅니다.
 * DATABASE_DESIGN.md의 user.status CHECK 제약조건과 일치합니다.
 * </p>
 *
 * <p>
 * 상태 전이 흐름:
 * PENDING → APPROVED (관리자 승인 시)
 * PENDING → REJECTED (관리자 거부 시)
 * </p>
 *
 * <ul>
 * <li>PENDING: 가입 승인 대기 중</li>
 * <li>APPROVED: 관리자 승인 완료 (로그인 가능)</li>
 * <li>REJECTED: 관리자 거부</li>
 * </ul>
 *
 * @author gayul.kim
 * @since 2026-03-07
 */
public enum UserStatus {

    /** 가입 승인 대기 중 (초기 상태) */
    PENDING,

    /** 관리자 승인 완료 (status=APPROVED & emailVerified=true 여야 로그인 가능) */
    APPROVED,

    /** 관리자 거부 */
    REJECTED
}
