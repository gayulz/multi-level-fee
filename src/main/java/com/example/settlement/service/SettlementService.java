package com.example.settlement.service;

import com.example.settlement.domain.entity.SettlementNode;
import com.example.settlement.dto.NodeCreateRequest;
import com.example.settlement.dto.SettlementRequest;
import com.example.settlement.dto.SettlementResult;
import com.example.settlement.domain.entity.User;
import com.example.settlement.dto.request.SettlementRequestDto;

import java.util.List;

/**
 * [NEW] 정산 서비스 인터페이스.
 *
 * 정산 비즈니스 로직 및 노드 관리에 대한 기능을 명세합니다.
 *
 * @author gayul.kim
 * @since 2026-03-06
 */
public interface SettlementService {

    /**
     * [NEW] 다단계 정산 계산.
     *
     * 주어진 요청 정보(루트 노드 ID, 거래 금액)를 바탕으로 트리 전체의 수수료 분배를 재귀적으로 계산하고
     * 루트 노드에 낙전(Dust)을 귀속시켜 최종 결과를 반환합니다.
     *
     * @param request 정산 요청 DTO
     * @return 정산 결과 트리 구조 DTO
     */
    SettlementResult calculate(SettlementRequest request);

    /**
     * [NEW] 새로운 정산 노드 생성.
     *
     * @param request 노드 생성 요청 DTO
     * @return 생성된 노드 Entity
     */
    SettlementNode createNode(NodeCreateRequest request);

    /**
     * [NEW] 기존 정산 노드 수정.
     *
     * @param id      수정할 노드 ID
     * @param request 수정 요청 DTO
     * @return 수정된 노드 Entity
     */
    SettlementNode updateNode(Long id, NodeCreateRequest request);

    /**
     * [NEW] 최상위 노드 목록 반환.
     *
     * @return 부모가 없는 루트 노드 목록
     */
    List<SettlementNode> getRootNodes();

    /**
     * [NEW] 사용자의 정산 요청 생성.
     */
    com.example.settlement.domain.entity.SettlementRequest createRequest(SettlementRequestDto dto, User requester);

    /**
     * [NEW] 정산 요청 단건 조회.
     */
    com.example.settlement.domain.entity.SettlementRequest getRequest(Long id);

    /**
     * [NEW] 정산 요청 상세 조회 (DTO 변환).
     *
     * <p>
     * 상세 페이지용으로 Entity를 SettlementDetailDto로 변환하여 반환합니다.
     * Fetch Join으로 organization, requester를 함께 로딩합니다.
     * </p>
     *
     * @param id 정산 요청 ID
     * @return 정산 상세 DTO
     * @author gayul.kim
     */
    com.example.settlement.dto.response.SettlementDetailDto getRequestDetail(Long id);

    /**
     * [MIG] 페이징된 전체 정산 요청 목록 조회 (SUPER_ADMIN용).
     *
     * @param pageable 페이징 정보
     * @return 페이징된 정산 요청 목록
     * @author gayul.kim
     */
    org.springframework.data.domain.Page<com.example.settlement.domain.entity.SettlementRequest> getAllRequests(org.springframework.data.domain.Pageable pageable);

    /**
     * [MIG] 페이징된 소속 조직 + 하위 조직의 정산 요청 목록 조회 (ADMIN용).
     *
     * @param orgId    소속 조직 ID
     * @param pageable 페이징 정보
     * @return 페이징된 정산 요청 목록
     * @author gayul.kim
     */
    org.springframework.data.domain.Page<com.example.settlement.domain.entity.SettlementRequest> getRequestsByOrganizationAndDescendants(Long orgId, org.springframework.data.domain.Pageable pageable);

    /**
     * [MIG] 페이징된 사용자의 정산 요청 목록 조회 (USER용).
     *
     * @param user     사용자
     * @param pageable 페이징 정보
     * @return 페이징된 정산 요청 목록
     * @author gayul.kim
     */
    org.springframework.data.domain.Page<com.example.settlement.domain.entity.SettlementRequest> getRequestsByUser(com.example.settlement.domain.entity.User user, org.springframework.data.domain.Pageable pageable);

    /**
     * [NEW] 전체 정산 요청 총 건수 조회 (SUPER_ADMIN 대시보드용).
     *
     * @author gayul.kim
     * @return 전체 정산 요청 수
     */
    long getTotalRequests();

    /**
     * [NEW] 최근 정산 요청 N 건 조회 (대시보드 요약 표 용).
     *
     * Fetch Join으로 organization, requester를 함께 로딩합니다.
     *
     * @author gayul.kim
     * @param limit 조회 건수
     * @return 정렬된 정산 요청 목록 (organization, requester 포함)
     */
    List<com.example.settlement.domain.entity.SettlementRequest> getRecentRequests(int limit);
}
