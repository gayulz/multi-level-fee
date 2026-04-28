package com.example.settlement.service.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * [NEW] No-op email sender — logs the message and returns success.
 *
 * <p>
 * Activated when {@code app.email.provider=noop} (also the default when the property is missing).
 * Lets the application boot and run end-to-end signup flows without an email API key,
 * which is essential for local development.
 * </p>
 *
 * <p>
 * The verification token is included in the log line so developers can copy it
 * directly into the verify URL without an actual email client.
 * </p>
 *
 * @author gayul.kim
 * @since 2026-04-28
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.email.provider", havingValue = "noop", matchIfMissing = true)
public class NoopEmailSender implements EmailSender {

	@Override
	public void send(EmailMessage message) {
		log.info("[EMAIL][NOOP] to={}, subject={}\n----- HTML BODY START -----\n{}\n----- HTML BODY END -----",
				message.to(), message.subject(), message.htmlBody());
	}

	@Override
	public String providerName() {
		return "noop";
	}
}
