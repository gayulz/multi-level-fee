package com.example.settlement.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * [NEW] 대시보드 응답 DTO
 *
 * 권한별 대시보드 통계 데이터를 전달한다.
 *
 * @author gayul.kim
 * @since 2025-01-30
 */
@Getter
@Builder
public class DashboardResponse {

	/**
	 * 통계 정보
	 */
	@Getter
	@Builder
	public static class Statistics {
		private Long totalRequests;           // 전체 정산 요청 수
		private Long pendingRequests;         // 승인 대기 중인 요청 수
		private Long approvedRequests;        // 승인 완료된 요청 수
		private Long rejectedRequests;        // 반려된 요청 수
		private BigDecimal totalAmount;       // 총 정산 금액
		private Long activeUsers;             // 활성 사용자 수
	}

	/**
	 * 최근 정산 요청
	 */
	@Getter
	@Builder
	public static class RecentSettlement {
		private Long id;
		private String orderId; // [MIG] 화면 노출용 주문 ID 추가
		private String requesterName;
		private String organizationName;
		private BigDecimal amount;
		private String status;
		private String statusText;
		private String createdAt;
	}

	/**
	 * 조직 트리 노드
	 */
	@Getter
	@Builder
	public static class OrganizationNode {
		private Long id;
		private String name;
		private String type;
		private Integer level;
		private Long requestCount;
		private List<OrganizationNode> children;
	}

	private Statistics statistics;                     // 통계 정보
	private List<RecentSettlement> recentSettlements; // 최근 정산 목록
	private OrganizationNode organizationTree;        // 조직 트리 (ADMIN 이상)
}
