package com.example.settlement.domain.repository;

import com.example.settlement.domain.entity.User;
import com.example.settlement.domain.entity.enums.UserRole;

import java.util.List;

/**
 * [NEW] 사용자 QueryDSL Custom Repository 인터페이스.
 *
 * @author gayul.kim
 * @since 2026-03-07
 */
public interface UserRepositoryCustom {

    /**
     * 특정 조직의 특정 권한 사용자 목록 조회.
     *
     * @param orgId 조직 ID
     * @param role  권한
     * @return 해당 조직 & 권한을 가진 사용자 목록
     */
    List<User> findByOrganizationAndRole(Long orgId, UserRole role);

    /**
     * 특정 조직의 가입 승인 대기 중인 사용자 목록 조회.
     *
     * @param orgId 조직 ID
     * @return status=PENDING인 사용자 목록
     */
    List<User> findPendingByOrganization(Long orgId);

    /**
     * 여러 조직의 사용자 목록 조회 (승인 처리 권한용).
     *
     * @param orgIds 조직 ID 목록
     * @return 해당 조직들에 속한 사용자 목록
     */
    List<User> findByOrganizationIn(List<Long> orgIds);
}
