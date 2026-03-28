package com.example.settlement.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * [NEW] 정산 요청 생성 DTO.
 *
 * @author gayul.kim
 * @since 2026-03-09
 */
public record SettlementRequestDto(
                @NotBlank(message = "주문 번호는 필수입니다") String orderId,

                @NotNull(message = "정산 금액은 필수입니다") @Min(value = 0, message = "정산 금액은 0 이상이어야 합니다") BigDecimal amount,

                @NotNull(message = "정산 노드 ID는 필수입니다") Long rootNodeId,

                String description) {
}
