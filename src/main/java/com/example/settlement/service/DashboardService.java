package com.example.settlement.service;

import com.example.settlement.domain.entity.Organization;
import com.example.settlement.domain.entity.SettlementRequest;
import com.example.settlement.domain.entity.User;
import com.example.settlement.domain.entity.enums.SettlementStatus;
import com.example.settlement.domain.entity.enums.UserRole;
import com.example.settlement.domain.entity.enums.UserStatus;
import com.example.settlement.domain.repository.OrganizationRepository;
import com.example.settlement.domain.repository.SettlementRequestRepository;
import com.example.settlement.domain.repository.UserRepository;
import com.example.settlement.dto.response.DashboardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * [NEW] 대시보드 서비스
 *
 * 권한별 대시보드 데이터를 조회한다.
 *
 * @author gayul.kim
 * @since 2025-01-30
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DashboardService {

	private final SettlementRequestRepository settlementRequestRepository;
	private final UserRepository userRepository;
	private final OrganizationRepository organizationRepository;

	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

	/**
	 * 권한별 대시보드 데이터를 조회한다
	 *
	 * @param user 현재 로그인한 사용자
	 * @return DashboardResponse 대시보드 데이터
	 */
	public DashboardResponse getDashboardData(User user) {
		// 권한에 따라 다른 통계 조회
		DashboardResponse.Statistics statistics = getStatistics(user);

		// 최근 정산 요청 조회 (최대 5개)
		List<DashboardResponse.RecentSettlement> recentSettlements = getRecentSettlements(user);

		// 조직 트리 (ADMIN 이상만)
		DashboardResponse.OrganizationNode organizationTree = null;
		if (user.getRole() == UserRole.ROLE_ADMIN || user.getRole() == UserRole.ROLE_SUPER_ADMIN) {
			organizationTree = getOrganizationTree(user);
		}

		return DashboardResponse.builder()
			.statistics(statistics)
			.recentSettlements(recentSettlements)
			.organizationTree(organizationTree)
			.build();
	}

	/**
	 * 권한별 통계 데이터를 조회한다
	 */
	private DashboardResponse.Statistics getStatistics(User user) {
		List<SettlementRequest> requests;

		if (user.getRole() == UserRole.ROLE_SUPER_ADMIN) {
			// SUPER_ADMIN: 전체 조회
			requests = settlementRequestRepository.findAll();
		} else if (user.getRole() == UserRole.ROLE_ADMIN) {
			// ADMIN: 소속 조직 + 하위 조직
			List<Long> orgIds = getOrgIdsIncludingChildren(user.getOrganization());
			requests = settlementRequestRepository.findByOrganizationOrgIdIn(orgIds);
		} else {
			// USER: 본인 요청만
			requests = settlementRequestRepository.findByRequesterUserId(user.getUserId());
		}

		Long totalRequests = (long) requests.size();
		Long pendingRequests = requests.stream()
			.filter(r -> r.getStatus() == SettlementStatus.PENDING)
			.count();
		Long approvedRequests = requests.stream()
			.filter(r -> r.getStatus() == SettlementStatus.AGENCY_APPROVED
				|| r.getStatus() == SettlementStatus.BRANCH_APPROVED
				|| r.getStatus() == SettlementStatus.COMPLETED)
			.count();
		Long rejectedRequests = requests.stream()
			.filter(r -> r.getStatus() == SettlementStatus.REJECTED)
			.count();

		BigDecimal totalAmount = requests.stream()
			.filter(r -> r.getStatus() == SettlementStatus.COMPLETED)
			.map(SettlementRequest::getAmount)
			.reduce(BigDecimal.ZERO, BigDecimal::add);

		// 활성 사용자 수
		Long activeUsers = getActiveUserCount(user);

		return DashboardResponse.Statistics.builder()
			.totalRequests(totalRequests)
			.pendingRequests(pendingRequests)
			.approvedRequests(approvedRequests)
			.rejectedRequests(rejectedRequests)
			.totalAmount(totalAmount)
			.activeUsers(activeUsers)
			.build();
	}

	/**
	 * 최근 정산 요청 목록을 조회한다 (최대 5개)
	 */
	private List<DashboardResponse.RecentSettlement> getRecentSettlements(User user) {
		List<SettlementRequest> requests;

		if (user.getRole() == UserRole.ROLE_SUPER_ADMIN) {
			requests = settlementRequestRepository.findTop5ByOrderByCreatedAtDesc(PageRequest.of(0, 5));
		} else if (user.getRole() == UserRole.ROLE_ADMIN) {
			List<Long> orgIds = getOrgIdsIncludingChildren(user.getOrganization());
			requests = settlementRequestRepository.findTop5ByOrganizationIdInOrderByCreatedAtDesc(orgIds, PageRequest.of(0, 5));
		} else {
			requests = settlementRequestRepository.findTop5ByRequesterIdOrderByCreatedAtDesc(user.getUserId(), PageRequest.of(0, 5));
		}

		return requests.stream()
			.map(this::toRecentSettlement)
			.collect(Collectors.toList());
	}

	/**
	 * 조직 트리를 조회한다 (ADMIN 이상)
	 */
	private DashboardResponse.OrganizationNode getOrganizationTree(User user) {
		Organization rootOrg;

		if (user.getRole() == UserRole.ROLE_SUPER_ADMIN) {
			// SUPER_ADMIN: 최상위 조직부터
			rootOrg = organizationRepository.findByParentIsNull().stream().findFirst().orElse(null);
		} else {
			// ADMIN: 소속 조직부터 (detached 프록시 대신 현재 세션에서 재조회)
			rootOrg = organizationRepository.findById(user.getOrganization().getOrgId()).orElse(null);
		}

		if (rootOrg == null) {
			return null;
		}

		return toOrganizationNode(rootOrg);
	}

	/**
	 * 조직과 하위 조직 ID 목록을 재귀적으로 수집한다
	 */
	private List<Long> getOrgIdsIncludingChildren(Organization org) {
		List<Long> ids = new ArrayList<>();
		ids.add(org.getOrgId());

		List<Organization> children = organizationRepository.findByParentOrgId(org.getOrgId());
		for (Organization child : children) {
			ids.addAll(getOrgIdsIncludingChildren(child));
		}

		return ids;
	}

	/**
	 * 활성 사용자 수를 조회한다
	 */
	private Long getActiveUserCount(User user) {
		if (user.getRole() == UserRole.ROLE_SUPER_ADMIN) {
			// SUPER_ADMIN: 전체 활성 사용자
			return userRepository.countByStatus(UserStatus.APPROVED);
		} else if (user.getRole() == UserRole.ROLE_ADMIN) {
			// ADMIN: 소속 조직 + 하위 조직 활성 사용자
			List<Long> orgIds = getOrgIdsIncludingChildren(user.getOrganization());
			return userRepository.countByOrganizationOrgIdInAndStatus(orgIds, UserStatus.APPROVED);
		} else {
			// USER: 본인만
			return 1L;
		}
	}

	/**
	 * SettlementRequest를 RecentSettlement DTO로 변환한다
	 */
	private DashboardResponse.RecentSettlement toRecentSettlement(SettlementRequest request) {
		return DashboardResponse.RecentSettlement.builder()
			.id(request.getId())
			.orderId(request.getOrderId())
			.requesterName(request.getRequester().getName())
			.organizationName(request.getOrganization().getOrgName())
			.amount(request.getAmount())
			.status(request.getStatus().name())
			.statusText(getStatusText(request.getStatus()))
			.createdAt(request.getCreatedAt().format(FORMATTER))
			.build();
	}

	/**
	 * Organization을 OrganizationNode DTO로 변환한다 (재귀)
	 */
	private DashboardResponse.OrganizationNode toOrganizationNode(Organization org) {
		// 해당 조직의 정산 요청 수
		Long requestCount = settlementRequestRepository.countByOrganizationOrgId(org.getOrgId());

		// 하위 조직 재귀 변환
		List<Organization> children = organizationRepository.findByParentOrgId(org.getOrgId());
		List<DashboardResponse.OrganizationNode> childNodes = children.stream()
			.map(this::toOrganizationNode)
			.collect(Collectors.toList());

		return DashboardResponse.OrganizationNode.builder()
			.id(org.getOrgId())
			.name(org.getOrgName())
			.type(org.getOrgType().name())
			.level(org.getLevel())
			.requestCount(requestCount)
			.children(childNodes)
			.build();
	}

	/**
	 * ApprovalStatus를 한글 텍스트로 변환한다
	 */
	private String getStatusText(SettlementStatus status) {
		switch (status) {
			case PENDING:
				return "승인 대기";
			case AGENCY_APPROVED:
				return "대리점 승인";
			case BRANCH_APPROVED:
				return "지사 승인";
			case REJECTED:
				return "반려됨";
			case COMPLETED:
				return "완료됨";
			default:
				return "알 수 없음";
		}
	}
}
