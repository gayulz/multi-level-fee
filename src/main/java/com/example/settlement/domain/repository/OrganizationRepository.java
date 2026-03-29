package com.example.settlement.domain.repository;

import com.example.settlement.domain.entity.Organization;
import com.example.settlement.domain.entity.enums.OrgType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * [NEW] 조직 JPA Repository.
 *
 * @author gayul.kim
 * @since 2026-03-07
 */
public interface OrganizationRepository extends JpaRepository<Organization, Long>, OrganizationRepositoryCustom {

    /**
     * [NEW] 루트 조직(본사) 단건 조회.
     *
     * @return parent가 null인 조직 (없으면 Optional.empty)
     * @author gayul.kim
     */
    Optional<Organization> findByParentIsNull();

    /**
     * 조직 코드로 조회.
     *
     * @param orgCode 조직 코드
     * @return 조직 (없으면 Optional.empty)
     */
    Optional<Organization> findByOrgCode(String orgCode);

    /**
     * 조직 유형별 조회.
     *
     * @param orgType 조직 유형
     * @return 해당 유형의 조직 목록
     */
    List<Organization> findByOrgType(OrgType orgType);

    /**
     * 조직 코드 존재 여부 확인 (중복 등록 방지).
     *
     * @param orgCode 조직 코드
     * @return 존재하면 true
     */
    boolean existsByOrgCode(String orgCode);

    /**
     * [NEW] 부모 조직 ID로 하위 조직 목록 조회.
     *
     * @param parentOrgId 부모 조직 ID
     * @return 하위 조직 목록
     * @author gayul.kim
     */
    List<Organization> findByParentOrgId(Long parentOrgId);
}
