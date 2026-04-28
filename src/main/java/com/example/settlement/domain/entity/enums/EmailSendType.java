package com.example.settlement.domain.entity.enums;

/**
 * [NEW] Email send type — used for rate limiting buckets and audit logs.
 *
 * @author gayul.kim
 * @since 2026-04-28
 */
public enum EmailSendType {

	/** Email verification on signup. */
	VERIFICATION,

	/** Password reset (future). */
	PASSWORD_RESET
}
