package com.example.settlement.domain.repository;

import com.example.settlement.domain.entity.Organization;
import com.example.settlement.domain.entity.User;
import com.example.settlement.domain.entity.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
     * 이메일로 사용자 조회 시 소속 조직을 Fetch Join으로 함께 로드 (로그인 N+1 방지).
     *
     * CustomUserDetailsService에서 loadUserByUsername() 호출 시
     * organization이 LAZY로 설정되어 있어 로그인마다 추가 쿼리가 발생하는 문제를 해결.
     * 한 번의 쿼리로 User + Organization을 함께 조회한다.
     *
     * @param email 이메일
     * @return User + Organization이 즉시 로드된 사용자 (없으면 Optional.empty)
     */
    @Query("SELECT u FROM User u JOIN FETCH u.organization WHERE u.email = :email")
    Optional<User> findByEmailWithOrganization(@Param("email") String email);

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

    /**
     * [NEW] 조직 ID 목록과 상태로 활성 사용자 수 카운팅.
     *
     * @param orgIds 조직 ID 목록
     * @param status 가입 상태
     * @return 사용자 수
     * @author gayul.kim
     */
    Long countByOrganizationOrgIdInAndStatus(
            List<Long> orgIds,
            UserStatus status
    );
}
