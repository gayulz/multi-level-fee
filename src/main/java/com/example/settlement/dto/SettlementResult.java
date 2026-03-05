package com.example.settlement.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * [NEW] 정산 결과 DTO.
 *
 * 정산 처리 후 트리 구조로 결과를 반환하기 위한 DTO입니다.
 * 하위 노드의 정산 결과를 재귀적으로 포함합니다.
 *
 * @param nodeId       노드 ID
 * @param nodeName     노드 이름
 * @param feeAmount    계산된 수수료 금액
 * @param childResults 하위 노드 정산 결과 목록
 *
 * @author gayul.kim
 * @since 2026-03-06
 */
public record SettlementResult(
        Long nodeId,
        String nodeName,
        BigDecimal feeAmount,
        List<SettlementResult> childResults) {
}
