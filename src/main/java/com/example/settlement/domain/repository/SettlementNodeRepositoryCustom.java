package com.example.settlement.domain.repository;

import com.example.settlement.domain.entity.SettlementNode;
import java.util.List;
import java.util.Optional;

/**
 * [NEW] 정산 노드 Custom Repository 인터페이스.
 *
 * QueryDSL을 이용해 복잡한 동적 쿼리를 처리하기 위한 기능 목록입니다.
 *
 * @author gayul.kim
 * @since 2026-03-06
 */
public interface SettlementNodeRepositoryCustom {

    /**
     * [NEW] 노드 및 하위 트리 연관 조회.
     *
     * N+1 문제를 방지하기 위해 자식 노드 컬렉션을 페치 조인(fetch join)으로 한 번에 조회합니다.
     *
     * @param id 조회할 노드의 ID
     * @return 조건에 맞는 노드 (Optional)
     */
    Optional<SettlementNode> findByIdWithChildren(Long id);

    /**
     * [NEW] 모든 루트 노드 조회.
     *
     * 부모가 없는 최상위 노드 목록을 조회합니다.
     *
     * @return 루트 노드 목록
     */
    List<SettlementNode> findAllRootNodes();
}
