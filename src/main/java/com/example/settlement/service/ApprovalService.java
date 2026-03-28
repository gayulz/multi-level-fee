package com.example.settlement.service;

import com.example.settlement.domain.entity.SettlementRequest;
import com.example.settlement.domain.entity.User;

import java.util.List;

/**
 * [NEW] 정산 요청 다단계 승인 처리 Service 인터페이스.
 *
 * @author gayul.kim
 * @since 2026-03-09
 */
public interface ApprovalService {
    void approve(Long requestId, User approver, String comment);

    void reject(Long requestId, User approver, String reason);

    List<SettlementRequest> getPendingRequestsForApprover(User approver);

    boolean canApprove(SettlementRequest request, User approver);

    int getRequiredApprovalLevel(User requester);
}
