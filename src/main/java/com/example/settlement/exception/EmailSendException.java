package com.example.settlement.exception;

/**
 * [NEW] Thrown when an email provider call fails (e.g. Resend HTTP error).
 *
 * @author gayul.kim
 * @since 2026-04-28
 */
public class EmailSendException extends RuntimeException {

	public EmailSendException(String message) {
		super(message);
	}

	public EmailSendException(String message, Throwable cause) {
		super(message, cause);
	}
}
