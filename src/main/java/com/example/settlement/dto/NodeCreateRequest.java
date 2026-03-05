package com.example.settlement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * [NEW] 노드 생성 API 요청용 DTO.
 *
 * 새로운 정산 노드를 생성할 때 필요한 정보를 전달합니다.
 *
 * @param name     노드 이름 (필수)
 * @param feeRate  수수료율 (필수)
 * @param parentId 부모 노드 ID (최상위 노드일 경우 null)
 *
 * @author gayul.kim
 * @since 2026-03-06
 */
public record NodeCreateRequest(
        @NotBlank(message = "노드 이름은 필수입니다.") String name,

        @NotNull(message = "수수료율은 필수입니다.") BigDecimal feeRate,

        Long parentId) {
}
