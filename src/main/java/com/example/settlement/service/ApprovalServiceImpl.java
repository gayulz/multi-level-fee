package com.example.settlement.service;

import com.example.settlement.domain.entity.SettlementRequest;
import com.example.settlement.domain.entity.User;
import com.example.settlement.domain.entity.enums.UserRole;
import com.example.settlement.domain.repository.SettlementRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * [NEW] 정산 요청 다단계 승인 처리 Service 구현체.
 *
 * @author gayul.kim
 * @since 2026-03-09
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApprovalServiceImpl implements ApprovalService {

    private final SettlementRequestRepository settlementRequestRepository;

    @Override
    @Transactional
    public void approve(Long requestId, User approver, String comment) {
        SettlementRequest request = settlementRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("정산 요청을 찾을 수 없습니다"));

        if (!canApprove(request, approver)) {
            throw new IllegalArgumentException("승인 권한이 없습니다");
        }

        request.approve(approver, comment);
        settlementRequestRepository.save(request);
    }

    @Override
    @Transactional
    public void reject(Long requestId, User approver, String reason) {
        SettlementRequest request = settlementRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("정산 요청을 찾을 수 없습니다"));

        if (!canApprove(request, approver)) {
            throw new IllegalArgumentException("승인/반려 권한이 없습니다");
        }

        request.reject(approver, reason);
        settlementRequestRepository.save(request);
    }

    @Override
    public boolean canApprove(SettlementRequest request, User approver) {
        int currentApprovalLevel = request.getCurrentApprovalLevel();
        int approverLevel = approver.getOrganization().getLevel();

        // currentApprovalLevel (1: 대리점 대기, 2: 지사 대기, 3: 본사 대기)
        // approverLevel (3: 대리점, 2: 지사, 1: 본사)
        // 합이 4일 때 매칭됨
        return (currentApprovalLevel + approverLevel == 4) && approver.hasRole(UserRole.ROLE_ADMIN);
    }

    @Override
    public int getRequiredApprovalLevel(User requester) {
        return requester.getOrganization().getLevel();
    }

    @Override
    public List<SettlementRequest> getPendingRequestsForApprover(User approver) {
        int approverLevel = approver.getOrganization().getLevel();
        // 대리점 관리자(level=3) -> approvalLevel=1 대기 목록 조회
        int requiredCurrentLevel = 4 - approverLevel;
        return settlementRequestRepository.findPendingByApprovalLevel(requiredCurrentLevel);
    }
}
