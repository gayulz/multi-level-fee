package com.example.settlement.common;

import jakarta.servlet.http.HttpServletRequest;

/**
 * [NEW] Best-effort client IP extractor that respects common reverse proxy headers.
 *
 * <p>
 * Order of precedence:
 * 1. {@code X-Forwarded-For} — first value in the comma-separated list (originating client)
 * 2. {@code X-Real-IP}        — common single-value alternative used by nginx
 * 3. {@code request.getRemoteAddr()} — direct connection peer
 * </p>
 *
 * <p>
 * Caveat: trust these headers ONLY when the deployment terminates behind a known reverse proxy
 * that strips/normalizes them. In a direct-internet deployment, attackers can spoof
 * X-Forwarded-For — the rate limiter therefore still applies email-dimension limits as a
 * second guard.
 * </p>
 *
 * @author gayul.kim
 * @since 2026-04-28
 */
public final class IpAddressExtractor {

	private static final String X_FORWARDED_FOR = "X-Forwarded-For";
	private static final String X_REAL_IP = "X-Real-IP";
	private static final String UNKNOWN = "unknown";

	private IpAddressExtractor() {
	}

	/**
	 * Extract the originating client IP from an HTTP request.
	 *
	 * @param request servlet request (must not be null)
	 * @return non-null IP string; falls back to {@code "unknown"} only if everything is empty
	 */
	public static String extract(HttpServletRequest request) {
		String forwarded = request.getHeader(X_FORWARDED_FOR);
		if (isPresent(forwarded)) {
			int comma = forwarded.indexOf(',');
			String first = (comma > 0) ? forwarded.substring(0, comma) : forwarded;
			return first.trim();
		}

		String realIp = request.getHeader(X_REAL_IP);
		if (isPresent(realIp)) {
			return realIp.trim();
		}

		String remote = request.getRemoteAddr();
		return isPresent(remote) ? remote : UNKNOWN;
	}

	private static boolean isPresent(String value) {
		return value != null && !value.isBlank() && !UNKNOWN.equalsIgnoreCase(value);
	}
}
