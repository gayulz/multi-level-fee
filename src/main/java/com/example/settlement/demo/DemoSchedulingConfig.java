package com.example.settlement.demo;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * [NEW] 데모 모드 전용 스케줄링 활성화 설정.
 *
 * <p>
 * app.demo.enabled=true 인 경우에만 @EnableScheduling 이 동작하도록 격리합니다.
 * 메인 애플리케이션 클래스(@SpringBootApplication)에 직접 @EnableScheduling 을
 * 붙이지 않는 이유는, 일반 운영 환경에서 데모 모드를 끄면 스케줄링 인프라
 * 자체가 로드되지 않게 만들기 위함입니다(YAGNI + 부수효과 최소화).
 * </p>
 *
 * @author gayul.kim
 * @since 2026-05-30
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "app.demo", name = "enabled", havingValue = "true")
public class DemoSchedulingConfig {
}
