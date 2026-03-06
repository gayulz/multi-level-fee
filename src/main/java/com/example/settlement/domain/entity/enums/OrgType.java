package com.example.settlement.domain.entity.enums;

/**
 * [NEW] 조직 유형 Enum.
 *
 * <p>
 * 조직의 계층 구조를 나타내는 유형입니다.
 * DATABASE_DESIGN.md의 organization.org_type CHECK 제약조건과 일치합니다.
 * </p>
 *
 * <ul>
 * <li>HEADQUARTERS: 본사 (level=1)</li>
 * <li>BRANCH: 지사 (level=2)</li>
 * <li>AGENCY: 대리점 (level=3)</li>
 * </ul>
 *
 * @author gayul.kim
 * @since 2026-03-07
 */
public enum OrgType {

    /** 본사 (최상위 조직, level=1) */
    HEADQUARTERS,

    /** 지사 (중간 조직, level=2) */
    BRANCH,

    /** 대리점 (말단 조직, level=3) */
    AGENCY
}
