package com.example.settlement.domain.repository;

import com.example.settlement.domain.entity.Organization;
import com.example.settlement.domain.entity.SettlementRequest;
import com.example.settlement.domain.entity.User;
import com.example.settlement.domain.entity.enums.SettlementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * [NEW] 정산 요청 JPA Repository.
 *
 * @author gayul.kim
 * @since 2026-03-07
 */
public interface SettlementRequestRepository
        extends JpaRepository<SettlementRequest, Long>, SettlementRequestRepositoryCustom {

    /**
     * 요청자별 정산 요청 목록 조회.
     *
     * @param requester 요청자
     * @return 해당 요청자의 정산 요청 목록
     */
    List<SettlementRequest> findByRequester(User requester);

    /**
     * 요청자별 정산 요청 목록 조회 (최신순).
     *
     * @param requester 요청자
     * @return 해당 요청자의 정산 요청 목록 (최신순)
     */
    List<SettlementRequest> findByRequesterOrderByCreatedAtDesc(User requester);

    /**
     * [NEW] 요청자별 정산 요청 목록 조회 (Fetch Join으로 organization, requester 함께 로딩).
     *
     * LazyInitializationException 방지를 위해 템플릿에서 사용할 연관 엔티티를 미리 로딩합니다.
     *
     * @param requester 요청자
     * @return 해당 요청자의 정산 요청 목록 (최신순)
     * @author gayul.kim
     */
    @Query("SELECT s FROM SettlementRequest s " +
           "JOIN FETCH s.organization " +
           "JOIN FETCH s.requester " +
           "WHERE s.requester = :requester " +
           "ORDER BY s.createdAt DESC")
    List<SettlementRequest> findByRequesterWithDetails(@Param("requester") User requester);

    /**
     * 승인 상태별 정산 요청 목록 조회.
     *
     * @param status 승인 상태
     * @return 해당 상태의 정산 요청 목록
     */
    List<SettlementRequest> findByStatus(SettlementStatus status);

    /**
     * 조직별 정산 요청 목록 조회.
     *
     * @param organization 조직
     * @return 해당 조직의 정산 요청 목록
     */
    List<SettlementRequest> findByOrganization(Organization organization);

    /**
     * [NEW] 조직별 정산 요청 목록 조회 (Fetch Join으로 organization, requester 함께 로딩).
     *
     * LazyInitializationException 방지를 위해 템플릿에서 사용할 연관 엔티티를 미리 로딩합니다.
     *
     * @param organization 조직
     * @return 해당 조직의 정산 요청 목록 (최신순)
     * @author gayul.kim
     */
    @Query("SELECT s FROM SettlementRequest s " +
           "JOIN FETCH s.organization " +
           "JOIN FETCH s.requester " +
           "WHERE s.organization = :organization " +
           "ORDER BY s.createdAt DESC")
    List<SettlementRequest> findByOrganizationWithDetails(@Param("organization") Organization organization);

    /**
     * [NEW] 최근 정산 요청 조회 (Fetch Join으로 organization, requester 함께 로딩, Page 반환).
     *
     * @param pageable 페이징 정보
     * @return 페이징된 정산 요청 목록
     * @author gayul.kim
     */
    @Query(value = "SELECT s FROM SettlementRequest s " +
           "JOIN FETCH s.organization " +
           "JOIN FETCH s.requester ",
           countQuery = "SELECT count(s) FROM SettlementRequest s")
    org.springframework.data.domain.Page<SettlementRequest> findAllWithDetails(org.springframework.data.domain.Pageable pageable);

    /**
     * [NEW] 조직 ID 목록으로 정산 요청 목록 조회 (Fetch Join, Page 반환).
     *
     * @param orgIds   조직 ID 목록
     * @param pageable 페이징 정보
     * @return 페이징된 정산 요청 목록
     * @author gayul.kim
     */
    @Query(value = "SELECT s FROM SettlementRequest s " +
           "JOIN FETCH s.organization org " +
           "JOIN FETCH s.requester " +
           "WHERE org.orgId IN :orgIds",
           countQuery = "SELECT count(s) FROM SettlementRequest s WHERE s.organization.orgId IN :orgIds")
    org.springframework.data.domain.Page<SettlementRequest> findByOrganizationOrgIdInWithDetails(@Param("orgIds") List<Long> orgIds, org.springframework.data.domain.Pageable pageable);

    /**
     * [NEW] 요청자별 정산 요청 목록 조회 (Fetch Join, Page 반환).
     *
     * @param requester 요청자
     * @param pageable  페이징 정보
     * @return 페이징된 정산 요청 목록
     * @author gayul.kim
     */
    @Query(value = "SELECT s FROM SettlementRequest s " +
           "JOIN FETCH s.organization " +
           "JOIN FETCH s.requester " +
           "WHERE s.requester = :requester",
           countQuery = "SELECT count(s) FROM SettlementRequest s WHERE s.requester = :requester")
    org.springframework.data.domain.Page<SettlementRequest> findByRequesterWithDetails(@Param("requester") User requester, org.springframework.data.domain.Pageable pageable);

    /**
     * [NEW] 정산 요청 단건 조회 (Fetch Join으로 organization, requester 함께 로딩).
     *
     * LazyInitializationException 방지를 위해 상세 페이지에서 사용할 연관 엔티티를 미리 로딩합니다.
     *
     * @param id 정산 요청 ID
     * @return 연관 엔티티가 로딩된 정산 요청
     * @author gayul.kim
     */
    @Query("SELECT s FROM SettlementRequest s " +
           "JOIN FETCH s.organization " +
           "JOIN FETCH s.requester " +
           "WHERE s.id = :id")
    java.util.Optional<SettlementRequest> findByIdWithDetails(@Param("id") Long id);

    /**
     * 주문 ID로 정산 요청 존재 여부 확인 (중복 요청 방지).
     *
     * @param orderId 주문 ID
     * @return 존재하면 true
     */
    boolean existsByOrderId(String orderId);

    /**
     * [NEW] 요청자 ID로 정산 요청 목록 조회.
     *
     * @param requesterId 요청자 ID
     * @return 해당 요청자의 정산 요청 목록
     * @author gayul.kim
     */
    List<SettlementRequest> findByRequesterUserId(Long requesterId);

    /**
     * [NEW] 조직 ID 목록으로 정산 요청 목록 조회 (Fetch Join으로 organization, requester 함께 로딩).
     *
     * @param orgIds 조직 ID 목록
     * @return 해당 조직들의 정산 요청 목록 (최신순)
     * @author gayul.kim
     */
    @Query("SELECT s FROM SettlementRequest s " +
           "JOIN FETCH s.organization org " +
           "JOIN FETCH s.requester " +
           "WHERE org.orgId IN :orgIds " +
           "ORDER BY s.createdAt DESC")
    List<SettlementRequest> findByOrganizationOrgIdInWithDetails(@Param("orgIds") List<Long> orgIds);

    /**
     * [NEW] 조직 ID 목록으로 정산 요청 목록 조회.
     *
     * @param orgIds 조직 ID 목록
     * @return 해당 조직들의 정산 요청 목록
     * @author gayul.kim
     */
    List<SettlementRequest> findByOrganizationOrgIdIn(List<Long> orgIds);

    /**
     * [NEW] 최근 정산 요청 조회 (전체, Pageable).
     *
     * @param pageable 페이징 정보
     * @return 최근 정산 요청 목록
     * @author gayul.kim
     */
    @Query("SELECT s FROM SettlementRequest s " +
           "JOIN FETCH s.organization " +
           "JOIN FETCH s.requester " +
           "ORDER BY s.createdAt DESC")
    List<SettlementRequest> findTop5ByOrderByCreatedAtDesc(org.springframework.data.domain.Pageable pageable);

    /**
     * [NEW] 조직 ID 목록으로 최근 정산 요청 조회 (Pageable).
     *
     * @param orgIds   조직 ID 목록
     * @param pageable 페이징 정보
     * @return 해당 조직들의 최근 정산 요청 목록
     * @author gayul.kim
     */
    @Query("SELECT s FROM SettlementRequest s " +
           "JOIN FETCH s.organization org " +
           "JOIN FETCH s.requester " +
           "WHERE org.orgId IN :orgIds " +
           "ORDER BY s.createdAt DESC")
    List<SettlementRequest> findTop5ByOrganizationIdInOrderByCreatedAtDesc(
            @Param("orgIds") List<Long> orgIds,
            org.springframework.data.domain.Pageable pageable
    );

    /**
     * [NEW] 요청자 ID로 최근 정산 요청 조회 (Pageable).
     *
     * @param requesterId 요청자 ID
     * @param pageable    페이징 정보
     * @return 해당 요청자의 최근 정산 요청 목록
     * @author gayul.kim
     */
    @Query("SELECT s FROM SettlementRequest s " +
           "JOIN FETCH s.organization " +
           "JOIN FETCH s.requester r " +
           "WHERE r.userId = :requesterId " +
           "ORDER BY s.createdAt DESC")
    List<SettlementRequest> findTop5ByRequesterIdOrderByCreatedAtDesc(
            @Param("requesterId") Long requesterId,
            org.springframework.data.domain.Pageable pageable
    );

    /**
     * [NEW] 조직 ID로 정산 요청 수 조회.
     *
     * @param orgId 조직 ID
     * @return 해당 조직의 정산 요청 수
     * @author gayul.kim
     */
    Long countByOrganizationOrgId(Long orgId);
}
