package com.example.settlement.service.impl;

import com.example.settlement.domain.entity.SettlementNode;
import com.example.settlement.domain.repository.SettlementNodeRepository;
import com.example.settlement.dto.NodeCreateRequest;
import com.example.settlement.dto.SettlementRequest;
import com.example.settlement.dto.SettlementResult;
import com.example.settlement.service.SettlementService;
import com.example.settlement.domain.entity.Organization;
import com.example.settlement.domain.entity.User;
import com.example.settlement.domain.repository.OrganizationRepository;
import com.example.settlement.domain.repository.SettlementRequestRepository;
import com.example.settlement.dto.request.SettlementRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * [NEW] 정산 서비스 구현체.
 *
 * 다단계 트리 순회를 통한 DFS 재귀 정산, 1/N 배분, 낙전 보정 등의
 * 핵심 비즈니스 로직을 수행합니다.
 *
 * @author gayul.kim
 * @since 2026-03-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementServiceImpl implements SettlementService {

    private final SettlementNodeRepository settlementNodeRepository;
    private final SettlementRequestRepository settlementRequestRepository;
    private final OrganizationRepository organizationRepository;

    @Override
    @Transactional
    public SettlementResult calculate(SettlementRequest request) {
        // 1. 루트 노드 조회 (fetchJoin으로 한 번에 트리 일부 조회)
        SettlementNode rootNode = settlementNodeRepository.findByIdWithChildren(request.rootNodeId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 노드입니다. ID: " + request.rootNodeId()));

        BigDecimal originalAmount = BigDecimal.valueOf(request.amount());

        // 2. DFS 재귀 기반 정산 및 1/N 분배 실행
        SettlementResult resultTree = calculateRecursive(rootNode, originalAmount);

        // 3. 총 지급 수수료 계산 (트리 전체 순회)
        BigDecimal totalAllocatedFee = calculateTotalFee(resultTree);

        // 4. 낙전(Dust) 보정: 차액을 루트 노드에 강제 귀속 (선택지 A 정책)
        BigDecimal dust = originalAmount.subtract(totalAllocatedFee);

        if (dust.compareTo(BigDecimal.ZERO) > 0) {
            log.info("정산 낙전({}) 발생 - 루트 노드({}) 수익으로 합산", dust, rootNode.getName());
            // Record는 불변 객체이므로 새로운 객체로 생성하여 반환
            return new SettlementResult(
                    resultTree.nodeId(),
                    resultTree.nodeName(),
                    resultTree.feeAmount().add(dust), // 루트 수수료 + 낙전
                    resultTree.childResults());
        }

        return resultTree;
    }

    @Override
    @Transactional
    public SettlementNode createNode(NodeCreateRequest request) {
        SettlementNode parent = null;
        if (request.parentId() != null) {
            parent = settlementNodeRepository.findById(Objects.requireNonNull(request.parentId()))
                    .orElseThrow(() -> new IllegalArgumentException("부모 노드가 존재하지 않습니다. ID: " + request.parentId()));
        }

        SettlementNode newNode = new SettlementNode(request.name(), request.feeRate());
        if (parent != null) {
            parent.addChild(newNode); // 연관관계 편의 메서드 호출
        }

        return settlementNodeRepository.save(newNode);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SettlementNode> getRootNodes() {
        return settlementNodeRepository.findAllRootNodes();
    }

    @Override
    @Transactional
    public com.example.settlement.domain.entity.SettlementRequest createRequest(SettlementRequestDto dto,
            User requester) {
        SettlementNode rootNode = settlementNodeRepository.findById(dto.rootNodeId())
                .orElseThrow(() -> new IllegalArgumentException("정산 노드를 찾을 수 없습니다"));

        // rootNode 연관 추후 구현일 수 있어서마 실제 요청에 rootNode는 저장하지 않습니다 (DTO에 rootNodeId를 통해 확인)
        // 평정서를 저장합니다
        com.example.settlement.domain.entity.SettlementRequest request = com.example.settlement.domain.entity.SettlementRequest
                .create(
                        dto.orderId(),
                        dto.amount(),
                        dto.description(),
                        requester,
                        requester.getOrganization());

        return settlementRequestRepository.save(request);
    }

    @Override
    @Transactional(readOnly = true)
    public long getTotalRequests() {
        return settlementRequestRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<com.example.settlement.domain.entity.SettlementRequest> getRecentRequests(int limit) {
        // Fetch Join으로 organization, requester 함께 로딩 (LazyInitializationException 방지)
        return settlementRequestRepository.findAllWithDetailsOrderByCreatedAtDesc().stream()
                .limit(limit)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public com.example.settlement.domain.entity.SettlementRequest getRequest(Long id) {
        return settlementRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("정산 요청을 찾을 수 없습니다"));
    }

    /**
     * [NEW] 정산 요청 상세 조회 (DTO 변환).
     *
     * <p>
     * Fetch Join으로 organization, requester를 함께 로딩한 뒤
     * SettlementDetailDto로 변환하여 반환합니다.
     * </p>
     *
     * @param id 정산 요청 ID
     * @return 정산 상세 DTO
     * @author gayul.kim
     */
    @Override
    @Transactional(readOnly = true)
    public com.example.settlement.dto.response.SettlementDetailDto getRequestDetail(Long id) {
        com.example.settlement.domain.entity.SettlementRequest entity =
                settlementRequestRepository.findByIdWithDetails(id)
                        .orElseThrow(() -> new IllegalArgumentException("정산 요청을 찾을 수 없습니다"));
        return com.example.settlement.dto.response.SettlementDetailDto.from(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<com.example.settlement.domain.entity.SettlementRequest> getRequestsByUser(User user) {
        // Fetch Join으로 organization, requester 함께 로딩 (LazyInitializationException 방지)
        return settlementRequestRepository.findByRequesterWithDetails(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<com.example.settlement.domain.entity.SettlementRequest> getRequestsByOrganization(Long orgId) {
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new IllegalArgumentException("조직을 찾을 수 없습니다"));
        // Fetch Join으로 organization, requester 함께 로딩 (LazyInitializationException 방지)
        return settlementRequestRepository.findByOrganizationWithDetails(org);
    }

    /**
     * DFS 재귀적으로 수수료를 계산하고 자식들에게 1/N 금액을 분할하여 호출합니다.
     */
    private SettlementResult calculateRecursive(SettlementNode node, BigDecimal remainingAmount) {
        // 1. 본인의 수수료 계산 (소수점 버림 처리)
        BigDecimal fee = node.calculateFee(remainingAmount);

        // 2. 본인 수수료를 떼고 남은 금액
        BigDecimal childRemainder = remainingAmount.subtract(fee);

        List<SettlementResult> childResults = new ArrayList<>();

        // 3. 자식이 있는 경우, 1/N 동일 분배하여 재귀 호출 (선택지 A 정책)
        if (!node.getChildren().isEmpty()) {
            BigDecimal childCount = BigDecimal.valueOf(node.getChildren().size());
            // 1/N 시 소수점 이하는 버림(FLOOR). 버려진 자투리(낙전)는 최상단 루트로 보정됨.
            BigDecimal childShare = childRemainder.divide(childCount, 0, RoundingMode.FLOOR);

            for (SettlementNode child : node.getChildren()) {
                childResults.add(calculateRecursive(child, childShare));
            }
        }

        return new SettlementResult(
                node.getId(),
                node.getName(),
                fee,
                childResults);
    }

    /**
     * 결과 트리를 순회하며 모든 노드에 지급될 총 수수료 합계를 구합니다.
     */
    private BigDecimal calculateTotalFee(SettlementResult result) {
        BigDecimal total = result.feeAmount();
        for (SettlementResult child : result.childResults()) {
            total = total.add(calculateTotalFee(child));
        }
        return total;
    }
}
