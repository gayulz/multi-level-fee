package com.example.settlement.service;

import com.example.settlement.domain.entity.SettlementRequest;
import com.example.settlement.domain.entity.User;
import com.example.settlement.domain.entity.enums.UserRole;
import com.example.settlement.domain.repository.SettlementRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * [NEW] 정산 요청 다단계 승인 처리 Service 구현체.
 *
 * <p>
 * 권한 체계:
 * - ROLE_ADMIN : 본인 조직 단계의 정상 승인만 가능 (대리점→지사→본사 단계별 결재)
 * - ROLE_SUPER_ADMIN : 사이트 운영자 권한, 모든 단계 강제 승인/반려 가능 (비상 처리용)
 * </p>
 *
 * @author gayul.kim
 * @since 2026-03-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApprovalServiceImpl implements ApprovalService {

    private final SettlementRequestRepository settlementRequestRepository;

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public void approve(Long requestId, User approver, String comment) {
        SettlementRequest request = settlementRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("정산 요청을 찾을 수 없습니다"));

        if (!canApprove(request, approver)) {
            throw new IllegalArgumentException("승인 권한이 없습니다");
        }

        logApprovalAudit("APPROVE", request, approver, comment);

        request.approve(approver, comment);
        settlementRequestRepository.save(request);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public void reject(Long requestId, User approver, String reason) {
        SettlementRequest request = settlementRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("정산 요청을 찾을 수 없습니다"));

        if (!canApprove(request, approver)) {
            throw new IllegalArgumentException("승인/반려 권한이 없습니다");
        }

        logApprovalAudit("REJECT", request, approver, reason);

        request.reject(approver, reason);
        settlementRequestRepository.save(request);
    }

    @Override
    public boolean canApprove(SettlementRequest request, User approver) {
        // SUPER_ADMIN: site operator with override authority for all stages
        if (approver.hasRole(UserRole.ROLE_SUPER_ADMIN)) {
            return true;
        }

        // ADMIN: stage-matched approval only
        // currentApprovalLevel (1: agency-pending, 2: branch-pending, 3: HQ-pending)
        // approverOrgLevel (3: agency, 2: branch, 1: HQ)
        // matched when sum equals 4
        int currentApprovalLevel = request.getCurrentApprovalLevel();
        int approverOrgLevel = approver.getOrganization().getLevel();
        return (currentApprovalLevel + approverOrgLevel == 4)
                && approver.hasRole(UserRole.ROLE_ADMIN);
    }

    /**
     * [NEW] Audit log for approval/rejection actions.
     * Distinguishes SUPER_ADMIN override actions from regular ADMIN approvals.
     *
     * @param action   action label ("APPROVE" or "REJECT")
     * @param request  target settlement request
     * @param approver acting user
     * @param message  comment or rejection reason
     * @author gayul.kim
     */
    private void logApprovalAudit(String action, SettlementRequest request, User approver, String message) {
        boolean isOverride = approver.hasRole(UserRole.ROLE_SUPER_ADMIN);
        if (isOverride) {
            log.warn("[AUDIT][SUPER_ADMIN_OVERRIDE] action={}, requestId={}, approver={}(id={}), currentLevel={}, status={}, message={}",
                    action,
                    request.getId(),
                    approver.getEmail(),
                    approver.getUserId(),
                    request.getCurrentApprovalLevel(),
                    request.getStatus(),
                    message);
        } else {
            log.info("[AUDIT][NORMAL] action={}, requestId={}, approver={}(id={}), orgLevel={}, currentLevel={}",
                    action,
                    request.getId(),
                    approver.getEmail(),
                    approver.getUserId(),
                    approver.getOrganization().getLevel(),
                    request.getCurrentApprovalLevel());
        }
    }

    @Override
    public int getRequiredApprovalLevel(User requester) {
        return requester.getOrganization().getLevel();
    }

    @Override
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public List<SettlementRequest> getPendingRequestsForApprover(User approver) {
        int approverLevel = approver.getOrganization().getLevel();
        // 대리점 관리자(level=3) -> approvalLevel=1 대기 목록 조회
        int requiredCurrentLevel = 4 - approverLevel;
        return settlementRequestRepository.findPendingByApprovalLevel(requiredCurrentLevel);
    }
}
