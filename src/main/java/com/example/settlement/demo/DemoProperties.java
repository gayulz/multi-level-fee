package com.example.settlement.demo;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * [NEW] 데모 모드 설정 프로퍼티.
 *
 * <p>
 * application.yml의 app.demo.* 설정을 매핑합니다.
 * - enabled: 데모 시드/엔드포인트 활성화 여부
 * - password: 4개 데모 계정에 공통 적용될 raw 비밀번호 (BCrypt 인코딩 전)
 * - reset-interval-minutes: PR2에서 사용 (정산 요청 자동 리셋 주기)
 * </p>
 *
 * @author gayul.kim
 * @since 2026-05-30
 */
@ConfigurationProperties(prefix = "app.demo")
public class DemoProperties {

	private boolean enabled = false;
	private String password = "demo1234!";
	private int resetIntervalMinutes = 30;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public int getResetIntervalMinutes() {
		return resetIntervalMinutes;
	}

	public void setResetIntervalMinutes(int resetIntervalMinutes) {
		this.resetIntervalMinutes = resetIntervalMinutes;
	}
}
