package com.example.settlement.service;

import com.example.settlement.domain.entity.SettlementNode;
import com.example.settlement.dto.NodeCreateRequest;
import com.example.settlement.dto.SettlementRequest;
import com.example.settlement.dto.SettlementResult;

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
     * [NEW] 최상위 노드 목록 반환.
     *
     * @return 부모가 없는 루트 노드 목록
     */
    List<SettlementNode> getRootNodes();
}
