package com.example.settlement.common;

/**
 * [NEW] Password policy single source of truth.
 *
 * <p>
 * Policy: at least 1 uppercase letter, at least 1 lowercase letter,
 * at least 1 special character, minimum length of 8 characters.
 * </p>
 *
 * <p>
 * Use {@link #REGEX} for backend validation (Bean Validation, manual matching)
 * and {@link #HTML_PATTERN} for HTML5 frontend pattern attribute.
 * Both expressions are functionally equivalent — keep them in sync.
 * </p>
 *
 * @author gayul.kim
 * @since 2026-04-28
 */
public final class PasswordPolicy {

	/**
	 * Backend password regex (Java String literal — escapes for backslash, brackets, quote).
	 * Used by {@code @Pattern} annotation and {@code String.matches(...)}.
	 */
	public static final String REGEX =
			"^(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).{8,}$";

	/**
	 * HTML5 pattern attribute compatible regex.
	 * Single-source for {@code <input pattern="...">} in Thymeleaf templates.
	 */
	public static final String HTML_PATTERN =
			"^(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':&quot;\\\\|,.<>/?]).{8,}$";

	/** Minimum required length. */
	public static final int MIN_LENGTH = 8;

	/** User-facing violation message (Korean — matches AI response language policy). */
	public static final String VIOLATION_MESSAGE =
			"비밀번호는 대문자, 소문자, 특수문자를 각 1자 이상 포함하여 최소 8자 이상이어야 합니다.";

	/** Title attribute hint for HTML5 input. */
	public static final String INPUT_TITLE =
			"대문자/소문자/특수문자 각 1자 이상, 최소 8자";

	private PasswordPolicy() {
		// utility class — no instantiation
	}

	/**
	 * Validates a raw password against the policy.
	 *
	 * @param rawPassword candidate password (must not be null)
	 * @return {@code true} if the password satisfies the policy
	 */
	public static boolean isValid(String rawPassword) {
		return rawPassword != null && rawPassword.matches(REGEX);
	}
}
