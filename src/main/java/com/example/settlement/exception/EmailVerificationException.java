package com.example.settlement.exception;

/**
 * [NEW] Thrown when an email verification token is invalid or expired.
 *
 * @author gayul.kim
 * @since 2026-04-28
 */
public class EmailVerificationException extends RuntimeException {

	public enum Reason {
		TOKEN_NOT_FOUND,
		TOKEN_EXPIRED,
		ALREADY_VERIFIED
	}

	private final Reason reason;

	public EmailVerificationException(Reason reason, String message) {
		super(message);
		this.reason = reason;
	}

	public Reason getReason() {
		return reason;
	}
}
