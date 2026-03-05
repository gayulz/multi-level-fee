package com.example.settlement.messaging;

import com.example.settlement.config.RabbitMqConfig;
import com.example.settlement.dto.SettlementRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * [NEW] RabbitMQ 정산 요청 메시지 발송용 프로듀서.
 *
 * 외부 연동 시뮬레이션 및 테스트 목적으로 사용되며, Exchange와 RoutingKey를 통해
 * 정산 요청 데이터를 JSON 메시지로 변환하여 큐에 발행합니다.
 *
 * @author gayul.kim
 * @since 2026-03-06
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementMessageProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * [NEW] 메시지 발송.
     *
     * @param request 발송할 정산 요청
     */
    public void send(SettlementRequest request) {
        log.info("정산 요청 발송: orderId={}, amount={}", request.orderId(), request.amount());
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.EXCHANGE_NAME,
                RabbitMqConfig.ROUTING_KEY,
                request);
        log.info("정산 요청 발송 완료");
    }
}
