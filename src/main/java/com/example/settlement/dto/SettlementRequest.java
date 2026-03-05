package com.example.settlement.dto;

/**
 * [NEW] 정산 요청 메시지용 DTO.
 *
 * RabbitMQ를 통해 전달되는 정산 요청 메시지 형식을 정의합니다.
 *
 * @param orderId    주문 ID
 * @param amount     정산 대상 금액
 * @param rootNodeId 정산을 시작할 최상위 노드 ID
 *
 * @author gayul.kim
 * @since 2026-03-06
 */
public record SettlementRequest(
        String orderId,
        Long amount,
        Long rootNodeId) {
}
