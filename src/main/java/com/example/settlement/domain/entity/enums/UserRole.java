package com.example.settlement.domain.entity.enums;

/**
 * [NEW] 사용자 권한 Enum.
 *
 * <p>
 * Spring Security의 GrantedAuthority와 호환되는 권한 열거형입니다.
 * DATABASE_DESIGN.md의 user.role CHECK 제약조건과 일치합니다.
 * </p>
 *
 * <ul>
 * <li>ROLE_SUPER_ADMIN: 최고 관리자 - 모든 권한 보유</li>
 * <li>ROLE_ADMIN: 관리자 - 소속 조직 및 하위 조직 관리</li>
 * <li>ROLE_USER: 일반 사용자 - 정산 요청 생성 가능</li>
 * </ul>
 *
 * @author gayul.kim
 * @since 2026-03-07
 */
public enum UserRole {

    /** 최고 관리자 (모든 조직 최상위 권한) */
    ROLE_SUPER_ADMIN,

    /** 관리자 (자신의 조직 및 하위 조직 관리 권한) */
    ROLE_ADMIN,

    /** 일반 사용자 (정산 요청 생성만 가능) */
    ROLE_USER
}
