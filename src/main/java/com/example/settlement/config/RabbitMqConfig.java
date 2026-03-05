package com.example.settlement.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * [NEW] RabbitMQ Direct Exchange 설정 클래스.
 *
 * 정산 요청 메시지를 수신하기 위한 Exchange, Queue, Binding을 설정합니다.
 * Jackson2JsonMessageConverter를 등록하여 JSON 메시지 자동 변환을 지원합니다.
 *
 * @author gayul.kim
 * @since 2026-03-06
 */
@Configuration
public class RabbitMqConfig {

    /** Exchange 이름 */
    public static final String EXCHANGE_NAME = "settlement.direct";

    /** Queue 이름 */
    public static final String QUEUE_NAME = "settlement.queue";

    /** Routing Key */
    public static final String ROUTING_KEY = "settlement.req";

    /**
     * [NEW] Direct Exchange Bean 등록.
     *
     * @return DirectExchange 정산용 Direct Exchange
     */
    @Bean
    public DirectExchange exchange() {
        return new DirectExchange(EXCHANGE_NAME);
    }

    /**
     * [NEW] Queue Bean 등록.
     *
     * @return Queue 정산 요청 큐
     */
    @Bean
    public Queue queue() {
        return new Queue(QUEUE_NAME);
    }

    /**
     * [NEW] Exchange-Queue 바인딩 Bean 등록.
     *
     * @param exchange Direct Exchange
     * @param queue    정산 요청 큐
     * @return Binding Exchange와 Queue를 Routing Key로 바인딩
     */
    @Bean
    public Binding binding(DirectExchange exchange, Queue queue) {
        return BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY);
    }

    /**
     * [NEW] JSON 메시지 변환기 Bean 등록.
     *
     * RabbitMQ 메시지를 JSON으로 자동 직렬화/역직렬화합니다.
     *
     * @return Jackson2JsonMessageConverter JSON 메시지 변환기
     */
    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
