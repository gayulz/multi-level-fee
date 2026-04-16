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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

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
    @PreAuthorize("hasRole('SUPER_ADMIN')")
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
                    resultTree.feeRate(),
                    resultTree.childResults());
        }

        return resultTree;
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public SettlementNode createNode(NodeCreateRequest request) {
        SettlementNode parentNode = null;
        Organization parentOrg = null;

        if (request.parentId() != null) {
            parentNode = settlementNodeRepository.findById(request.parentId())
                    .orElseThrow(() -> new IllegalArgumentException("상위 노드를 찾을 수 없습니다. ID: " + request.parentId()));
            
            // 수수료율 역전 확인 (하위가 상위보다 높을 수 없음)
            if (request.feeRate().compareTo(parentNode.getFeeRate()) > 0) {
                throw new IllegalArgumentException("하위 노드의 수수료율은 상위 노드의 수수료율(" + parentNode.getFeeRate() + "%)보다 클 수 없습니다.");
            }
            parentOrg = parentNode.getOrganization();
        }

        // 1. Organization 생성
        Organization newOrg;
        if (parentOrg == null) {
            newOrg = Organization.createHeadquarters(request.name(), "HQ-" + System.currentTimeMillis());
        } else {
            if (parentOrg.getLevel() == 1) {
                newOrg = Organization.createBranch(request.name(), "BR-" + System.currentTimeMillis(), parentOrg);
            } else if (parentOrg.getLevel() == 2) {
                newOrg = Organization.createAgency(request.name(), "AG-" + System.currentTimeMillis(), parentOrg);
            } else {
                throw new IllegalArgumentException("대리점 하위에는 더 이상 노드(조직)를 생성할 수 없습니다.");
            }
        }
        
        newOrg = organizationRepository.save(newOrg);

        // 2. SettlementNode 생성 및 매핑
        SettlementNode newNode;
        if (parentNode == null) {
            newNode = SettlementNode.createRoot(request.name(), newOrg, request.feeRate());
        } else {
            newNode = SettlementNode.createChild(request.name(), newOrg, request.feeRate(), parentNode);
        }

        return settlementNodeRepository.save(newNode);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public List<SettlementNode> getRootNodes() {
        return settlementNodeRepository.findAllRootNodes();
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('USER')")
    public com.example.settlement.domain.entity.SettlementRequest createRequest(SettlementRequestDto dto,
            User requester) {
        SettlementNode rootNode = settlementNodeRepository.findById(dto.rootNodeId())
                .orElseThrow(() -> new IllegalArgumentException("정산 노드를 찾을 수 없습니다"));

        com.example.settlement.domain.entity.SettlementRequest request = com.example.settlement.domain.entity.SettlementRequest
                .create(
                        dto.orderId(),
                        dto.amount(),
                        dto.description(),
                        requester,
                        requester.getOrganization(),
                        rootNode);

        return settlementRequestRepository.save(request);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public long getTotalRequests() {
        return settlementRequestRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public java.util.List<com.example.settlement.domain.entity.SettlementRequest> getRecentRequests(int limit) {
        // [MIG] Fetch Join과 결합된 Pageable을 사용하여 DB 레벨에서 최적화된 최신 내역 N개 조회
        return settlementRequestRepository.findTop5ByOrderByCreatedAtDesc(
                org.springframework.data.domain.PageRequest.of(0, limit)
        );
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
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
    @PreAuthorize("isAuthenticated()")
    public com.example.settlement.dto.response.SettlementDetailDto getRequestDetail(Long id) {
        com.example.settlement.domain.entity.SettlementRequest entity =
                settlementRequestRepository.findByIdWithDetails(id)
                        .orElseThrow(() -> new IllegalArgumentException("정산 요청을 찾을 수 없습니다"));

        List<com.example.settlement.dto.response.SettlementDetailDto.FeeDetailDto> feeDetails = new ArrayList<>();
        BigDecimal rootDust = BigDecimal.ZERO;
        
        // 정산 요청에 연결된 rootNode가 있다면, 이를 바탕으로 상세 내역 트리를 계산하여 평탄화
        if (entity.getRootNode() != null) {
            SettlementResult result = calculateRecursive(entity.getRootNode(), entity.getAmount());
            
            // [NEW] 트리의 배분 과정에서 발생한 모든 낙전 합산
            rootDust = calculateTotalDust(entity.getRootNode(), entity.getAmount());

            // 낙전이 발생했다면 루트 노드(본사)의 수수료 수익에 강제 병합하여 보정
            if (rootDust.compareTo(BigDecimal.ZERO) > 0) {
                result = new SettlementResult(
                        result.nodeId(),
                        result.nodeName(),
                        result.feeAmount().add(rootDust),
                        result.feeRate(),
                        result.childResults()
                );
            }

            // DTO 평탄화 처리 (0: 루트 뎁스)
            flattenSettlementResult(result, 0, feeDetails);
        }

        // DTO 반환 (rootDust 포함)
        return com.example.settlement.dto.response.SettlementDetailDto.from(entity, feeDetails, rootDust);
    }

    /**
     * [NEW] DFS 계산 결과를 1차원 리스트로 평탄화 (depth 포함).
     *
     * @param result 현재 노드의 정산 결과
     * @param depth 뎁스 (0: 본사, 1: 지사, 2: 대리점)
     * @param output 결과물 리스트
     */
    private void flattenSettlementResult(SettlementResult result, int depth, List<com.example.settlement.dto.response.SettlementDetailDto.FeeDetailDto> output) {
        BigDecimal percentage = result.feeRate().multiply(new BigDecimal("100")).setScale(2, java.math.RoundingMode.HALF_UP);
        String rateStr = percentage.toPlainString() + "%";

        output.add(new com.example.settlement.dto.response.SettlementDetailDto.FeeDetailDto(
                result.nodeName(),
                depth,
                rateStr,
                result.feeAmount()
        ));

        for (SettlementResult child : result.childResults()) {
            flattenSettlementResult(child, depth + 1, output);
        }
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("#user.userId == principal.userId or hasRole('SUPER_ADMIN')")
    public org.springframework.data.domain.Page<com.example.settlement.domain.entity.SettlementRequest> getRequestsByUser(User user, org.springframework.data.domain.Pageable pageable) {
        // [MIG] Fetch Join과 Pageable을 사용하여 DB 레벨 페이징 처리 (LazyInitializationException 방지)
        return settlementRequestRepository.findByRequesterWithDetails(user, pageable);
    }

    /**
     * [MIG] 전체 정산 요청 목록 조회 (SUPER_ADMIN용).
     *
     * @param pageable 페이징 정보
     * @return 전체 정산 요청 목록 (최신순)
     * @author gayul.kim
     */
    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public org.springframework.data.domain.Page<com.example.settlement.domain.entity.SettlementRequest> getAllRequests(org.springframework.data.domain.Pageable pageable) {
        return settlementRequestRepository.findAllWithDetails(pageable);
    }

    /**
     * [MIG] 소속 조직 + 하위 조직의 정산 요청 목록 조회 (ADMIN용).
     *
     * <p>
     * Organization의 Self-Reference Tree를 순회하여 하위 조직 ID 목록을 수집하고,
     * 해당 조직 ID 목록으로 정산 요청을 페이징 조회합니다.
     * </p>
     *
     * @param orgId    소속 조직 ID
     * @param pageable 페이징 정보
     * @return 해당 조직 및 하위 조직의 정산 요청 목록 (최신순)
     * @author gayul.kim
     */
    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN') and #orgId == principal.organization.orgId")
    public org.springframework.data.domain.Page<com.example.settlement.domain.entity.SettlementRequest> getRequestsByOrganizationAndDescendants(Long orgId, org.springframework.data.domain.Pageable pageable) {
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new IllegalArgumentException("조직을 찾을 수 없습니다"));

        // 자신 + 모든 하위 조직 ID 수집
        List<Long> orgIds = new ArrayList<>();
        orgIds.add(org.getOrgId());
        for (Organization descendant : org.getAllDescendants()) {
            orgIds.add(descendant.getOrgId());
        }

        return settlementRequestRepository.findByOrganizationOrgIdInWithDetails(orgIds, pageable);
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
                node.getFeeRate(),
                childResults);
    }

    private BigDecimal calculateTotalFee(SettlementResult result) {
        BigDecimal total = result.feeAmount();
        for (SettlementResult child : result.childResults()) {
            total = total.add(calculateTotalFee(child));
        }
        return total;
    }

    /**
     * [NEW] 서브트리 순회를 통해 각 배분 과정에서 발생한 소수점 절삭(낙전) 비용의 총합을 구합니다.
     * @author gayul.kim
     */
    private BigDecimal calculateTotalDust(SettlementNode node, BigDecimal remainingAmount) {
        BigDecimal fee = node.calculateFee(remainingAmount);
        BigDecimal childRemainder = remainingAmount.subtract(fee);
        BigDecimal totalDust = BigDecimal.ZERO;

        if (!node.getChildren().isEmpty()) {
            BigDecimal childCount = BigDecimal.valueOf(node.getChildren().size());
            BigDecimal childShare = childRemainder.divide(childCount, 0, java.math.RoundingMode.FLOOR);
            
            // 현재 노드에서 자식들에게 나눠주고 남은 자투리 기록
            BigDecimal currentDust = childRemainder.subtract(childShare.multiply(childCount));
            totalDust = totalDust.add(currentDust);
            
            for (SettlementNode child : node.getChildren()) {
                totalDust = totalDust.add(calculateTotalDust(child, childShare));
            }
        }
        return totalDust;
    }
}
