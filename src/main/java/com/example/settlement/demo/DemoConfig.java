package com.example.settlement.demo;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * [NEW] 데모 모드 관련 빈 설정.
 *
 * <p>
 * DemoProperties를 활성화합니다. 이 설정은 운영 환경 영향을 최소화하기 위해
 * demo 패키지 내부에 격리되며, app.demo.enabled=false 인 경우 시드/엔드포인트는
 * 동작하지 않습니다(개별 컴포넌트에서 가드 처리).
 * </p>
 *
 * @author gayul.kim
 * @since 2026-05-30
 */
@Configuration
@EnableConfigurationProperties(DemoProperties.class)
public class DemoConfig {
}
