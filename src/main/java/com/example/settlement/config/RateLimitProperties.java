package com.example.settlement.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * [NEW] Rate limit policy properties.
 *
 * <p>
 * Bound from {@code app.rate-limit.email-verification.*}.
 * Two dimensions are enforced independently — the request fails if EITHER limit is exceeded.
 * </p>
 *
 * @author gayul.kim
 * @since 2026-04-28
 */
@ConfigurationProperties(prefix = "app.rate-limit.email-verification")
public class RateLimitProperties {

	private int emailHourlyLimit = 5;
	private int emailDailyLimit = 10;
	private int ipHourlyLimit = 20;
	private int ipDailyLimit = 100;

	public int getEmailHourlyLimit() {
		return emailHourlyLimit;
	}

	public void setEmailHourlyLimit(int emailHourlyLimit) {
		this.emailHourlyLimit = emailHourlyLimit;
	}

	public int getEmailDailyLimit() {
		return emailDailyLimit;
	}

	public void setEmailDailyLimit(int emailDailyLimit) {
		this.emailDailyLimit = emailDailyLimit;
	}

	public int getIpHourlyLimit() {
		return ipHourlyLimit;
	}

	public void setIpHourlyLimit(int ipHourlyLimit) {
		this.ipHourlyLimit = ipHourlyLimit;
	}

	public int getIpDailyLimit() {
		return ipDailyLimit;
	}

	public void setIpDailyLimit(int ipDailyLimit) {
		this.ipDailyLimit = ipDailyLimit;
	}
}
