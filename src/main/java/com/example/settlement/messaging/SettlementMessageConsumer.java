package com.example.settlement.messaging;

import com.example.settlement.config.RabbitMqConfig;
import com.example.settlement.dto.SettlementRequest;
import com.example.settlement.dto.SettlementResult;
import com.example.settlement.service.SettlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * [NEW] RabbitMQ 정산 요청 메시지 컨슈머.
 *
 * 큐에서 정산 요청 메시지를 수신하여 비즈니스 로직(SettlementService)으로 전달합니다.
 *
 * @author gayul.kim
 * @since 2026-03-06
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementMessageConsumer {

    private final SettlementService settlementService;

    /**
     * [NEW] 정산 요청 큐 수신부.
     *
     * JSON 형식의 메시지를 수신하여 SettlementRequest DTO로 자동 역직렬화하고 정산을 수행합니다.
     *
     * @param request 수신된 정산 요청 메시지
     */
    @RabbitListener(queues = RabbitMqConfig.QUEUE_NAME)
    public void handleSettlementRequest(SettlementRequest request) {
        log.info("정산 요청 수신: orderId={}, amount={}, rootNodeId={}",
                request.orderId(), request.amount(), request.rootNodeId());

        SettlementResult result = settlementService.calculate(request);

        log.info("정산 완료: {}", result);
    }
}
