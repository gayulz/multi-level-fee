package com.example.settlement.service;

import com.example.settlement.domain.entity.User;
import com.example.settlement.domain.entity.enums.UserRole;
import com.example.settlement.dto.request.SignupRequest;

import java.util.List;

/**
 * [NEW] 사용자 관리 서비스 인터페이스
 *
 * @author gayul.kim
 * @since 2026-03-09
 */
public interface UserService {
    User registerUser(SignupRequest request);

    User getUserById(Long id);

    User getUserByEmail(String email);

    List<User> getUsersByOrganization(Long orgId);

    List<User> getPendingUsers(Long orgId);

    void approveUser(Long userId, Long approverId);

    void rejectUser(Long userId, Long approverId, String reason);

    void changeUserRole(Long userId, UserRole role);

    /**
     * [NEW] 전체 회원 목록 조회 (SUPER_ADMIN 전용).
     *
     * @author gayul.kim
     * @return 전체 사용자 목록
     */
    List<User> getAllUsers();

    /**
     * [NEW] 전체 모든 조직의 승인 대기 회원 목록 조회 (SUPER_ADMIN 전용).
     *
     * @author gayul.kim
     * @return 승인 대기 사용자 목록
     */
    List<User> getAllPendingUsers();

    /**
     * [NEW] 활성 사용자 수 조회 (APPROVED 상태).
     *
     * @author gayul.kim
     * @return 활성 사용자 수
     */
    long getActiveUsersCount();
}
