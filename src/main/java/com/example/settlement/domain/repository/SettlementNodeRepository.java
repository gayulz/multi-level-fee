package com.example.settlement.domain.repository;

import com.example.settlement.domain.entity.SettlementNode;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * [NEW] 정산 노드 Repository.
 *
 * Spring Data JPA의 기본 CRUD 기능과 QueryDSL을 이용한 Custom 기능을 모두 제공합니다.
 *
 * @author gayul.kim
 * @since 2026-03-06
 */
public interface SettlementNodeRepository
        extends JpaRepository<SettlementNode, Long>, SettlementNodeRepositoryCustom {
}
