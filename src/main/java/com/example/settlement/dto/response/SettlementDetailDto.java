package com.example.settlement.dto.response;

import com.example.settlement.domain.entity.SettlementRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * [NEW] 정산 상세 페이지 응답 전용 DTO.
 *
 * <p>
 * detail.html 템플릿이 필요로 하는 모든 필드를 포함합니다.
 * Entity를 직접 View에 노출하지 않고 DTO로 변환하여 전달합니다.
 * </p>
 *
 * @param id           정산 요청 ID
 * @param orderId      주문 ID
 * @param amount       결제 금액
 * @param totalFee     총 수수료 (Entity.feeAmount 매핑, null → BigDecimal.ZERO)
 * @param rootNodeName 루트 노드명 (Entity.organization.orgName 매핑)
 * @param status       정산 상태 (SettlementStatus.name())
 * @param settledAt    정산 완료 일시 (Entity.completedAt 매핑)
 * @param remark       비고 (Entity.description 매핑)
 * @param feeDetails   노드별 수수료 배분 내역
 *
 * @author gayul.kim
 * @since 2026-03-28
 */
public record SettlementDetailDto(
		Long id,
		String orderId,
		BigDecimal amount,
		BigDecimal totalFee,
		String rootNodeName,
		String status,
		LocalDateTime settledAt,
		String remark,
		List<FeeDetailDto> feeDetails) {

	/**
	 * [NEW] 노드별 수수료 배분 상세 DTO.
	 *
	 * <p>
	 * detail.html의 feeDetails 반복문에서 사용합니다.
	 * </p>
	 *
	 * @param nodeName 노드명
	 * @param depth    트리 깊이 (0: 본사, 1: 지사, 2: 대리점)
	 * @param rate     수수료율 (예: "10.00%")
	 * @param fee      수수료 금액
	 *
	 * @author gayul.kim
	 * @since 2026-03-28
	 */
	public record FeeDetailDto(
			String nodeName,
			int depth,
			String rate,
			BigDecimal fee) {
	}

	/**
	 * [NEW] Entity → DTO 변환 정적 팩토리 메서드.
	 *
	 * <p>
	 * SettlementRequest Entity의 필드를 detail.html 템플릿이
	 * 참조하는 필드명으로 매핑합니다.
	 * </p>
	 *
	 * @param entity 정산 요청 Entity (organization Fetch Join 필수)
	 * @return 변환된 SettlementDetailDto
	 * @author gayul.kim
	 */
	public static SettlementDetailDto from(SettlementRequest entity) {
		BigDecimal totalFee = entity.getFeeAmount() != null
				? entity.getFeeAmount()
				: BigDecimal.ZERO;

		String rootNodeName = entity.getOrganization() != null
				? entity.getOrganization().getOrgName()
				: "알 수 없음";

		return new SettlementDetailDto(
				entity.getId(),
				entity.getOrderId(),
				entity.getAmount(),
				totalFee,
				rootNodeName,
				entity.getStatus().name(),
				entity.getCompletedAt(),
				entity.getDescription(),
				Collections.emptyList());
	}
}
