package com.example.settlement.service.email;

import com.example.settlement.exception.EmailSendException;

/**
 * [NEW] Provider-agnostic email sending abstraction.
 *
 * <p>
 * Implementations are wired by Spring's {@code @ConditionalOnProperty}
 * based on {@code app.email.provider}. Higher-level services depend on this
 * interface only — switching providers is a config change, not a code change.
 * </p>
 *
 * @author gayul.kim
 * @since 2026-04-28
 */
public interface EmailSender {

	/**
	 * Sends an email synchronously.
	 *
	 * @param message payload to send
	 * @throws EmailSendException when the provider call fails or returns a non-2xx response
	 */
	void send(EmailMessage message) throws EmailSendException;

	/**
	 * @return identifier of the active provider — useful for audit logs
	 */
	String providerName();
}
