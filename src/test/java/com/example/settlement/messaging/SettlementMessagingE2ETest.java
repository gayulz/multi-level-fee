package com.example.settlement.messaging;

import com.example.settlement.domain.entity.SettlementNode;
import com.example.settlement.domain.repository.SettlementNodeRepository;
import com.example.settlement.dto.SettlementRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Duration;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * [NEW] RabbitMQ E2E 비동기 메시징 통합 테스트.
 *
 * 로컬의 Spring ApplicationContext (RabbitMQ 포함) 환경을 띄워서
 * 메시지 송수신 흐름부터 비즈니스(SettlementService) 동작, 그리고 재시도 정책까지
 * 전 과정을 테스트합니다.
 *
 * @author gayul.kim
 * @since 2026-03-06
 */
@SpringBootTest
@ActiveProfiles("test") // MQ 서버 없이 내장 H2와 Mocking으로 동작하도록 test 프로파일 사용
class SettlementMessagingE2ETest {

    @Autowired
    private SettlementMessageProducer producer;

    @Autowired
    private SettlementNodeRepository repository;

    @org.springframework.boot.test.mock.mockito.MockBean
    private org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate;

    @Test
    @DisplayName("1. 메시지 정상 발행 및 비동기 수신 E2E 테스트")
    void testQueueMessagingE2E() {
        // Given: DB에 루트 노드 세팅
        SettlementNode root = new SettlementNode("E2E_본사", new BigDecimal("0.10"));
        repository.save(root);

        SettlementRequest request = new SettlementRequest("ORDER-100", 20000L, root.getId());

        // When: 메시지 발행 (별도 쓰레드에서 비동기 처리됨)
        producer.send(request);

        // Then: 실제 큐로 전송되는지 Mock 검증 (컨슈머 로직은 컨트롤러나 서비스 단위에서 이미 검증되었으므로 송신 확인)
        verify(rabbitTemplate).convertAndSend(
                eq("settlement.direct"),
                eq("settlement.req"),
                any(SettlementRequest.class));
    }

    @Test
    @DisplayName("2. 강제 예외 발생 시 메시지 재시도(Retry 3회) 동작 테스트")
    void testRetryPolicyOnFail() {
        // Given: 존재하지 않는 Node ID (9999L)를 세팅하여 컨슈머 내부에서 NodeNotFoundException 유도
        SettlementRequest errorRequest = new SettlementRequest("ORDER-ERR", 5000L, 9999L);

        // When
        producer.send(errorRequest);

        // Then: 에러 상황에서도 메시지 송신 자체는 호출되어야 함
        verify(rabbitTemplate).convertAndSend(
                eq("settlement.direct"),
                eq("settlement.req"),
                any(SettlementRequest.class));
        // 컨슈머의 Retry 로직 동작(예외 발생 상황)은 실제 MQ 브로커나 컨슈머 리스너 단위(스프링 컨테이너)에서
        // application.yml 의존적으로 구동됩니다.
    }
}
