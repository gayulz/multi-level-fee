package com.example.settlement.exception;

/**
 * [NEW] Thrown when a rate limit is exceeded.
 *
 * @author gayul.kim
 * @since 2026-04-28
 */
public class RateLimitExceededException extends RuntimeException {

	private final String dimension;
	private final long retryAfterSeconds;

	public RateLimitExceededException(String dimension, long retryAfterSeconds, String message) {
		super(message);
		this.dimension = dimension;
		this.retryAfterSeconds = retryAfterSeconds;
	}

	public String getDimension() {
		return dimension;
	}

	public long getRetryAfterSeconds() {
		return retryAfterSeconds;
	}
}
