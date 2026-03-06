package com.example.settlement.domain.entity;

import com.example.settlement.domain.entity.enums.SettlementStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * [NEW] 정산 요청 Entity.
 *
 * <p>
 * 사용자의 정산 요청 정보 및 다단계 승인 프로세스를 추적합니다.
 * </p>
 *
 * <p>
 * 승인 단계 흐름 (대리점 요청 기준):
 * PENDING(level=1) → AGENCY_APPROVED(level=2) → BRANCH_APPROVED(level=3) →
 * COMPLETED
 * 어느 단계에서든 → REJECTED 처리 가능
 * </p>
 *
 * <p>
 * currentApprovalLevel 의미:
 * 1: 대리점 관리자(level=3) 승인 대기
 * 2: 지사 관리자(level=2) 승인 대기
 * 3: 본사 관리자(level=1) 승인 대기
 * </p>
 *
 * @author gayul.kim
 * @since 2026-03-07
 */
@Entity
@Table(name = "settlement_request")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SettlementRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id")
    private Long requestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @Column(name = "order_id", nullable = false, length = 100)
    private String orderId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "fee_amount", precision = 15, scale = 2)
    private BigDecimal feeAmount;

    @Column(name = "final_amount", precision = 15, scale = 2)
    private BigDecimal finalAmount;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SettlementStatus status = SettlementStatus.PENDING;

    @Column(name = "current_approval_level", nullable = false)
    private Integer currentApprovalLevel = 1;

    // =========================================================
    // 대리점 관리자 승인 정보 (level=3 조직 관리자)
    // =========================================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agency_approved_by")
    private User agencyApprovedBy;

    @Column(name = "agency_approved_at")
    private LocalDateTime agencyApprovedAt;

    @Column(name = "agency_comment")
    private String agencyComment;

    // =========================================================
    // 지사 관리자 승인 정보 (level=2 조직 관리자)
    // =========================================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_approved_by")
    private User branchApprovedBy;

    @Column(name = "branch_approved_at")
    private LocalDateTime branchApprovedAt;

    @Column(name = "branch_comment")
    private String branchComment;

    // =========================================================
    // 본사 관리자 승인 정보 (level=1 조직 관리자)
    // =========================================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hq_approved_by")
    private User hqApprovedBy;

    @Column(name = "hq_approved_at")
    private LocalDateTime hqApprovedAt;

    @Column(name = "hq_comment")
    private String hqComment;

    // =========================================================
    // 반려 정보
    // =========================================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rejected_by")
    private User rejectedBy;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "reject_reason", columnDefinition = "TEXT")
    private String rejectReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // =========================================================
    // 정적 팩토리 메서드
    // =========================================================

    /**
     * [NEW] 정산 요청 생성.
     *
     * <p>
     * 초기 상태: status=PENDING, currentApprovalLevel=1
     * </p>
     *
     * @param orderId      주문 ID
     * @param amount       정산 요청 금액
     * @param description  정산 설명
     * @param requester    요청자
     * @param organization 요청자 소속 조직
     * @return 초기 상태의 SettlementRequest
     * @author gayul.kim
     */
    public static SettlementRequest create(
            String orderId,
            BigDecimal amount,
            String description,
            User requester,
            Organization organization) {
        SettlementRequest request = new SettlementRequest();
        request.orderId = orderId;
        request.amount = amount;
        request.description = description;
        request.requester = requester;
        request.organization = organization;
        request.status = SettlementStatus.PENDING;
        request.currentApprovalLevel = 1;
        return request;
    }

    // =========================================================
    // 비즈니스 메서드 - 승인 프로세스
    // =========================================================

    /**
     * [NEW] 정산 요청 승인 처리.
     *
     * <p>
     * 승인자의 조직 레벨에 따라 상태가 전이됩니다:
     * <ul>
     * <li>level=3 (대리점 관리자): PENDING → AGENCY_APPROVED, currentApprovalLevel=2</li>
     * <li>level=2 (지사 관리자): AGENCY_APPROVED → BRANCH_APPROVED,
     * currentApprovalLevel=3</li>
     * <li>level=1 (본사 관리자): BRANCH_APPROVED → COMPLETED</li>
     * </ul>
     * </p>
     *
     * @param approver 승인자 (ROLE_ADMIN 권한)
     * @param comment  승인 코멘트
     * @throws IllegalStateException 이미 완료/반려된 요청이거나 권한 없는 승인 시
     * @author gayul.kim
     */
    public void approve(User approver, String comment) {
        if (this.status == SettlementStatus.COMPLETED || this.status == SettlementStatus.REJECTED) {
            throw new IllegalStateException("이미 완료되었거나 반려된 정산 요청입니다.");
        }

        Integer approverLevel = approver.getOrganization().getLevel();

        switch (approverLevel) {
            case 3: // 대리점 관리자
                this.agencyApprovedBy = approver;
                this.agencyApprovedAt = LocalDateTime.now();
                this.agencyComment = comment;
                this.status = SettlementStatus.AGENCY_APPROVED;
                this.currentApprovalLevel = 2;
                break;
            case 2: // 지사 관리자
                this.branchApprovedBy = approver;
                this.branchApprovedAt = LocalDateTime.now();
                this.branchComment = comment;
                this.status = SettlementStatus.BRANCH_APPROVED;
                this.currentApprovalLevel = 3;
                break;
            case 1: // 본사 관리자
                this.hqApprovedBy = approver;
                this.hqApprovedAt = LocalDateTime.now();
                this.hqComment = comment;
                this.status = SettlementStatus.COMPLETED;
                this.completedAt = LocalDateTime.now();
                break;
            default:
                throw new IllegalStateException("유효하지 않은 조직 레벨입니다: " + approverLevel);
        }
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * [NEW] 정산 요청 반려 처리.
     *
     * @param rejecter 반려자
     * @param reason   반려 사유
     * @throws IllegalStateException 이미 완료/반려된 요청 시
     * @author gayul.kim
     */
    public void reject(User rejecter, String reason) {
        if (this.status == SettlementStatus.COMPLETED || this.status == SettlementStatus.REJECTED) {
            throw new IllegalStateException("이미 완료되었거나 반려된 정산 요청입니다.");
        }
        this.rejectedBy = rejecter;
        this.rejectedAt = LocalDateTime.now();
        this.rejectReason = reason;
        this.status = SettlementStatus.REJECTED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * [NEW] 수수료 및 최종 정산 금액 설정.
     *
     * <p>
     * 정산 완료(COMPLETED) 처리 시 SettlementService에서 호출합니다.
     * </p>
     *
     * @param feeAmount   계산된 수수료
     * @param finalAmount 최종 정산 금액
     * @author gayul.kim
     */
    public void setSettlementAmounts(BigDecimal feeAmount, BigDecimal finalAmount) {
        this.feeAmount = feeAmount;
        this.finalAmount = finalAmount;
        this.updatedAt = LocalDateTime.now();
    }

    // =========================================================
    // 비즈니스 메서드 - 상태 확인
    // =========================================================

    /**
     * [NEW] 대기 중 여부 확인.
     *
     * @return status가 PENDING이면 true
     * @author gayul.kim
     */
    public boolean isPending() {
        return this.status == SettlementStatus.PENDING;
    }

    /**
     * [NEW] 완료 여부 확인.
     *
     * @return status가 COMPLETED이면 true
     * @author gayul.kim
     */
    public boolean isCompleted() {
        return this.status == SettlementStatus.COMPLETED;
    }

    /**
     * [NEW] 반려 여부 확인.
     *
     * @return status가 REJECTED이면 true
     * @author gayul.kim
     */
    public boolean isRejected() {
        return this.status == SettlementStatus.REJECTED;
    }

    // =========================================================
    // 생명주기 콜백
    // =========================================================

    @PrePersist
    private void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    private void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
