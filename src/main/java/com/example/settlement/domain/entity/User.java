package com.example.settlement.domain.entity;

import com.example.settlement.domain.entity.enums.UserRole;
import com.example.settlement.domain.entity.enums.UserStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * [NEW] 사용자 Entity.
 *
 * <p>
 * 시스템 사용자 정보 및 권한을 관리합니다.
 * 이메일 인증 + 관리자 승인 이중 절차를 통해 계정이 활성화됩니다.
 * </p>
 *
 * <p>
 * 로그인 가능 조건: status=APPROVED AND emailVerified=true
 * </p>
 *
 * <p>
 * 주의: 테이블명을 "users"로 지정. PostgreSQL에서 "user"는 예약어이므로 충돌 방지.
 * </p>
 *
 * @author gayul.kim
 * @since 2026-03-07
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    /** BCrypt 암호화된 비밀번호. 평문 저장 금지 - Service 레이어에서 인코딩 후 저장. */
    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 20)
    private String phone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Column(name = "privacy_agreed", nullable = false)
    private Boolean privacyAgreed = false;

    @Column(name = "email_verified", nullable = false)
    private Boolean emailVerified = false;

    @Column(name = "email_verification_token", length = 255)
    private String emailVerificationToken;

    @Column(name = "email_verification_expired_at")
    private LocalDateTime emailVerificationExpiredAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status = UserStatus.PENDING;

    @Column(name = "requested_at")
    private LocalDateTime requestedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column(name = "reject_reason", columnDefinition = "TEXT")
    private String rejectReason;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // =========================================================
    // 정적 팩토리 메서드
    // =========================================================

    /**
     * [NEW] 일반 사용자 생성 (가입 요청 상태).
     *
     * <p>
     * 초기 상태: role=ROLE_USER, status=PENDING, emailVerified=false
     * </p>
     *
     * @param email           이메일 (로그인 ID)
     * @param encodedPassword BCrypt 암호화된 비밀번호
     * @param name            사용자명
     * @param phone           연락처
     * @param organization    소속 조직
     * @param privacyAgreed   개인정보 동의 여부
     * @return 가입 요청 상태의 User
     * @author gayul.kim
     */
    public static User createUser(
            String email,
            String encodedPassword,
            String name,
            String phone,
            Organization organization,
            boolean privacyAgreed) {
        User user = new User();
        user.email = email;
        user.password = encodedPassword;
        user.name = name;
        user.phone = phone;
        user.organization = organization;
        user.role = UserRole.ROLE_USER;
        user.privacyAgreed = privacyAgreed;
        user.emailVerified = false;
        user.status = UserStatus.PENDING;
        user.active = true;
        user.requestedAt = LocalDateTime.now();
        return user;
    }

    /**
     * [NEW] 초기 데이터 세팅용 SUPER_ADMIN 생성 (앱 기동 시 자동 생성).
     *
     * <p>
     * DataInitializer에서만 사용합니다.
     * 바로 로그인 가능한 활성 상태(APPROVED, emailVerified=true)로 생성됩니다.
     * </p>
     *
     * @param email           이메일
     * @param encodedPassword BCrypt 암호화된 비밀번호
     * @param name            이름
     * @param phone           연락처
     * @param organization    소속 조직
     * @return 로그인 가능한 SUPER_ADMIN User
     * @author gayul.kim
     */
    public static User createSuperAdmin(
            String email,
            String encodedPassword,
            String name,
            String phone,
            Organization organization) {
        User user = new User();
        user.email = email;
        user.password = encodedPassword;
        user.name = name;
        user.phone = phone;
        user.organization = organization;
        user.role = UserRole.ROLE_SUPER_ADMIN;
        user.privacyAgreed = true;
        user.emailVerified = true; // 이싹 인증 통과
        user.status = UserStatus.APPROVED; // 승인 완료
        user.active = true;
        user.approvedAt = LocalDateTime.now();
        user.requestedAt = LocalDateTime.now();
        return user;
    }

    // =========================================================
    // 비즈니스 메서드 - 이메일 인증
    // =========================================================

    /**
     * [NEW] 이메일 인증 토큰 설정 (만료 5분).
     *
     * @param token 이메일 인증 토큰
     * @author gayul.kim
     */
    public void setEmailVerificationToken(String token) {
        this.emailVerificationToken = token;
        this.emailVerificationExpiredAt = LocalDateTime.now().plusMinutes(5);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * [NEW] 이메일 인증 완료 처리.
     *
     * @author gayul.kim
     */
    public void verifyEmail() {
        this.emailVerified = true;
        this.emailVerificationToken = null;
        this.emailVerificationExpiredAt = null;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * [NEW] 이메일 인증 토큰 유효 여부 확인.
     *
     * @return 토큰이 존재하고 만료 전이면 true
     * @author gayul.kim
     */
    public boolean isEmailVerificationTokenValid() {
        return this.emailVerificationToken != null
                && this.emailVerificationExpiredAt != null
                && LocalDateTime.now().isBefore(this.emailVerificationExpiredAt);
    }

    // =========================================================
    // 비즈니스 메서드 - 가입 승인
    // =========================================================

    /**
     * [NEW] 가입 승인 처리.
     *
     * @param approver 승인자 (관리자)
     * @author gayul.kim
     */
    public void approve(User approver) {
        this.status = UserStatus.APPROVED;
        this.approvedAt = LocalDateTime.now();
        this.approvedBy = approver;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * [NEW] 가입 거부 처리.
     *
     * @param reason 거부 사유
     * @author gayul.kim
     */
    public void reject(String reason) {
        this.status = UserStatus.REJECTED;
        this.rejectReason = reason;
        this.updatedAt = LocalDateTime.now();
    }

    // =========================================================
    // 비즈니스 메서드 - 계정 상태
    // =========================================================

    /**
     * [NEW] 로그인 가능 여부 확인.
     *
     * <p>
     * 로그인 조건: status=APPROVED AND emailVerified=true AND active=true
     * </p>
     *
     * @return 로그인 가능하면 true
     * @author gayul.kim
     */
    public boolean isLoginable() {
        return this.status == UserStatus.APPROVED
                && Boolean.TRUE.equals(this.emailVerified)
                && Boolean.TRUE.equals(this.active);
    }

    /**
     * [NEW] 특정 권한 보유 여부 확인.
     *
     * @param role 확인할 권한
     * @return 권한이 일치하면 true
     * @author gayul.kim
     */
    public boolean hasRole(UserRole role) {
        return this.role == role;
    }

    /**
     * [NEW] 사용자 권한 변경.
     *
     * @param role 변경할 권한
     * @author gayul.kim
     */
    public void changeRole(UserRole role) {
        this.role = role;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * [NEW] 특정 조직 소속 여부 확인.
     *
     * @param org 확인할 조직
     * @return 소속 조직이 일치하면 true
     * @author gayul.kim
     */
    public boolean isInOrganization(Organization org) {
        return this.organization.getOrgId().equals(org.getOrgId());
    }

    /**
     * [NEW] 마지막 로그인 시각 갱신.
     *
     * @author gayul.kim
     */
    public void updateLastLoginAt() {
        this.lastLoginAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * [NEW] 계정 비활성화.
     *
     * @author gayul.kim
     */
    public void deactivate() {
        this.active = false;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * [NEW] 비밀번호 변경.
     *
     * <p>
     * Service 레이어에서 BCrypt 인코딩 후 호출해야 합니다.
     * </p>
     *
     * @param encodedPassword BCrypt 암호화된 새 비밀번호
     * @author gayul.kim
     */
    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
        this.updatedAt = LocalDateTime.now();
    }

    // =========================================================
    // 생명주기 콜백
    // =========================================================

    @PrePersist
    private void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.requestedAt == null) {
            this.requestedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    private void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
