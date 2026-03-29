package com.example.settlement.domain.repository;

import com.example.settlement.domain.entity.Organization;
import com.example.settlement.domain.entity.User;
import com.example.settlement.domain.entity.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * [NEW] 사용자 JPA Repository.
 *
 * @author gayul.kim
 * @since 2026-03-07
 */
public interface UserRepository extends JpaRepository<User, Long>, UserRepositoryCustom {

    /**
     * 이메일로 사용자 조회 (로그인 시 사용).
     *
     * @param email 이메일
     * @return 사용자 (없으면 Optional.empty)
     */
    Optional<User> findByEmail(String email);

    /**
     * 소속 조직으로 사용자 목록 조회.
     *
     * @param organization 소속 조직
     * @return 해당 조직의 사용자 목록
     */
    List<User> findByOrganization(Organization organization);

    /**
     * 가입 상태로 사용자 목록 조회.
     *
     * @param status 가입 상태
     * @return 해당 상태의 사용자 목록
     */
    List<User> findByStatus(UserStatus status);

    /**
     * [MIG] 가입 상태로 활성 사용자 수 카운팅 (JPA 카운트 쿼리 최적화).
     *
     * @param status 가입 상태
     * @return 사용자 수
     * @author gayul.kim
     * @since 2026-03-29
     */
    long countByStatus(UserStatus status);

    /**
     * 이메일 중복 여부 확인 (회원가입 시 사용).
     *
     * @param email 이메일
     * @return 존재하면 true
     */
    boolean existsByEmail(String email);

    /**
     * 이메일 인증 토큰으로 사용자 조회.
     *
     * @param token 이메일 인증 토큰
     * @return 사용자 (없으면 Optional.empty)
     */
    Optional<User> findByEmailVerificationToken(String token);
}
