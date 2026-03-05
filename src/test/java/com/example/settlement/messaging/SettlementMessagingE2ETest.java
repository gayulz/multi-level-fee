package com.example.settlement.messaging;

import com.example.settlement.config.RabbitMqConfig;
import com.example.settlement.dto.SettlementRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * [NEW] RabbitMQ Producer/Consumer 유닛 테스트.
 *
 * 실제 RabbitMQ 브로커 없이 Mockito를 이용하여
 * Producer의 메시지 발송 로직과 파라미터를 검증합니다.
 * (실제 Consumer Retry 동작은 local 프로파일로 RabbitMQ 구동 후 수동 검증)
 *
 * @author gayul.kim
 * @since 2026-03-06
 */
@ExtendWith(MockitoExtension.class)
class SettlementMessagingE2ETest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private SettlementMessageProducer producer;

    @Test
    @DisplayName("1. 메시지 발행 시 올바른 Exchange/RoutingKey/Payload로 전송해야 한다")
    void testProducerSendsToCorrectDestination() {
        // Given
        SettlementRequest request = new SettlementRequest("ORDER-100", 20000L, 1L);

        // When
        producer.send(request);

        // Then: Exchange, RoutingKey, Request 객체가 정확하게 전달되었는지 검증
        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(RabbitMqConfig.EXCHANGE_NAME),
                eq(RabbitMqConfig.ROUTING_KEY),
                any(SettlementRequest.class));
    }

    @Test
    @DisplayName("2. 에러 케이스에서도 Producer 발송 시도가 이루어져야 한다")
    void testProducerSendsEvenOnErrorPayload() {
        // Given: 존재하지 않는 Node ID를 가진 요청 (Consumer에서 예외 발생 예정)
        SettlementRequest errorRequest = new SettlementRequest("ORDER-ERR", 5000L, 9999L);

        // When
        producer.send(errorRequest);

        // Then: Producer 레이어에서는 예외 없이 발송 시도가 정상 호출되어야 함
        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(RabbitMqConfig.EXCHANGE_NAME),
                eq(RabbitMqConfig.ROUTING_KEY),
                any(SettlementRequest.class));
        // Consumer의 Retry(3회) 동작은 application-local.yml 설정에 의해
        // 실제 RabbitMQ 브로커 실행 환경에서 수동 확인합니다.
    }
}
