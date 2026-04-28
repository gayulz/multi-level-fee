package com.example.settlement.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * [NEW] Email-related configuration properties.
 *
 * <p>
 * Bound from {@code app.email.*} in application.yml.
 * The {@code provider} switch determines which {@link com.example.settlement.service.email.EmailSender}
 * implementation is activated:
 * - {@code noop}   : log-only (default, safe for dev/local without API key)
 * - {@code resend} : Resend HTTP API
 * </p>
 *
 * @author gayul.kim
 * @since 2026-04-28
 */
@ConfigurationProperties(prefix = "app.email")
public class EmailProperties {

	/** Provider switch: "noop" or "resend". Default "noop" so the app boots safely without an API key. */
	private String provider = "noop";

	/** From address for outgoing mail. Should be on a domain you control + verified at the provider. */
	private String from = "noreply@settletree.io";

	/** Public base URL used to build verification links sent in emails. */
	private String publicBaseUrl = "http://localhost:8080";

	/** Verification token TTL in minutes. */
	private int verificationTtlMinutes = 30;

	/** Resend-specific settings (only used when provider=resend). */
	private final Resend resend = new Resend();

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public String getPublicBaseUrl() {
		return publicBaseUrl;
	}

	public void setPublicBaseUrl(String publicBaseUrl) {
		this.publicBaseUrl = publicBaseUrl;
	}

	public int getVerificationTtlMinutes() {
		return verificationTtlMinutes;
	}

	public void setVerificationTtlMinutes(int verificationTtlMinutes) {
		this.verificationTtlMinutes = verificationTtlMinutes;
	}

	public Resend getResend() {
		return resend;
	}

	public static class Resend {
		/** Resend API key (Bearer token). Read from RESEND_API_KEY env. */
		private String apiKey = "";

		/** Resend API base URL. */
		private String apiUrl = "https://api.resend.com/emails";

		/** HTTP connect/read timeout in milliseconds. */
		private int timeoutMillis = 5000;

		public String getApiKey() {
			return apiKey;
		}

		public void setApiKey(String apiKey) {
			this.apiKey = apiKey;
		}

		public String getApiUrl() {
			return apiUrl;
		}

		public void setApiUrl(String apiUrl) {
			this.apiUrl = apiUrl;
		}

		public int getTimeoutMillis() {
			return timeoutMillis;
		}

		public void setTimeoutMillis(int timeoutMillis) {
			this.timeoutMillis = timeoutMillis;
		}
	}
}
