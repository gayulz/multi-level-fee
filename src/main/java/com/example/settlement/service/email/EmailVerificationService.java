package com.example.settlement.service.email;

import com.example.settlement.config.EmailProperties;
import com.example.settlement.domain.entity.EmailSendLog;
import com.example.settlement.domain.entity.User;
import com.example.settlement.domain.entity.enums.EmailSendType;
import com.example.settlement.domain.repository.EmailSendLogRepository;
import com.example.settlement.domain.repository.UserRepository;
import com.example.settlement.exception.EmailSendException;
import com.example.settlement.exception.EmailVerificationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * [NEW] Coordinates email-verification token issuance, delivery, and consumption.
 *
 * <p>
 * Responsibilities:
 * 1. Generate cryptographically random tokens (32 bytes, URL-safe Base64).
 * 2. Persist the token + expiration on the {@link User}.
 * 3. Apply rate-limit checks (delegated to {@link RateLimitService}) before sending.
 * 4. Send the email via the configured {@link EmailSender}.
 * 5. Audit every attempt to {@link EmailSendLog}.
 * 6. Verify tokens and mark accounts as verified.
 * </p>
 *
 * <p>
 * Audit rule: a successful send is recorded with {@link EmailSendLog#success}, while
 * provider failures are recorded with {@link EmailSendLog#failure} BEFORE the exception
 * is rethrown. The rate-limit counter is incremented only on success — a failed attempt
 * does not consume the user's hourly budget.
 * </p>
 *
 * @author gayul.kim
 * @since 2026-04-28
 */
@Slf4j
@Service
public class EmailVerificationService {

	private static final int TOKEN_BYTES = 32;
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private final UserRepository userRepository;
	private final EmailSendLogRepository emailSendLogRepository;
	private final EmailSender emailSender;
	private final RateLimitService rateLimitService;
	private final EmailProperties emailProperties;

	public EmailVerificationService(
			UserRepository userRepository,
			EmailSendLogRepository emailSendLogRepository,
			EmailSender emailSender,
			RateLimitService rateLimitService,
			EmailProperties emailProperties) {
		this.userRepository = userRepository;
		this.emailSendLogRepository = emailSendLogRepository;
		this.emailSender = emailSender;
		this.rateLimitService = rateLimitService;
		this.emailProperties = emailProperties;
	}

	/**
	 * Issues a fresh verification token and dispatches the email.
	 *
	 * <p>
	 * Idempotent for the user: re-calling overwrites the previous token and resets TTL.
	 * Rate-limiter is consulted BEFORE token issuance so an abusive client can be
	 * stopped without polluting the DB with unsent tokens.
	 * </p>
	 *
	 * @param user       target user (must be persisted — must have an id)
	 * @param ipAddress  client IP for rate-limit and audit
	 * @throws com.example.settlement.exception.RateLimitExceededException when limit is hit
	 * @throws EmailSendException when the provider call fails
	 */
	@Transactional
	public void sendVerificationEmail(User user, String ipAddress) {
		String email = user.getEmail();
		EmailSendType sendType = EmailSendType.VERIFICATION;

		// 1. Rate-limit guard (throws RateLimitExceededException)
		rateLimitService.checkBeforeSend(email, ipAddress, sendType);

		// 2. Generate token + persist on user
		String token = generateToken();
		user.setEmailVerificationToken(token, emailProperties.getVerificationTtlMinutes());
		userRepository.save(user);

		// 3. Build payload
		String verifyUrl = buildVerificationUrl(token);
		EmailMessage message = new EmailMessage(
				email,
				"[SettleTree] 이메일 인증을 완료해주세요",
				renderHtml(user.getName(), verifyUrl, emailProperties.getVerificationTtlMinutes())
		);

		// 4. Send + audit
		try {
			emailSender.send(message);
			emailSendLogRepository.save(EmailSendLog.success(email, ipAddress, sendType));
			rateLimitService.recordSuccessfulSend(email, ipAddress, sendType);
			log.info("[AUDIT][EMAIL_SEND] success email={} provider={} type={}",
					email, emailSender.providerName(), sendType);
		} catch (EmailSendException ex) {
			emailSendLogRepository.save(EmailSendLog.failure(email, ipAddress, sendType, ex.getMessage()));
			log.warn("[AUDIT][EMAIL_SEND] failure email={} provider={} reason={}",
					email, emailSender.providerName(), ex.getMessage());
			throw ex;
		}
	}

	/**
	 * Verifies a token and marks the user's email as verified.
	 *
	 * @param token the URL token sent in the email
	 * @return the verified user (for caller to use in success messages)
	 * @throws EmailVerificationException if the token does not exist, has expired, or the user is already verified
	 */
	@Transactional
	public User verifyToken(String token) {
		User user = userRepository.findByEmailVerificationToken(token)
				.orElseThrow(() -> new EmailVerificationException(
						EmailVerificationException.Reason.TOKEN_NOT_FOUND,
						"인증 링크가 유효하지 않습니다. 인증 메일을 다시 발송해주세요."));

		if (Boolean.TRUE.equals(user.getEmailVerified())) {
			throw new EmailVerificationException(
					EmailVerificationException.Reason.ALREADY_VERIFIED,
					"이미 인증이 완료된 계정입니다.");
		}

		if (!user.isEmailVerificationTokenValid()) {
			throw new EmailVerificationException(
					EmailVerificationException.Reason.TOKEN_EXPIRED,
					"인증 링크가 만료되었습니다. 인증 메일을 다시 발송해주세요.");
		}

		user.verifyEmail();
		userRepository.save(user);
		log.info("[AUDIT][EMAIL_VERIFY] success email={}", user.getEmail());
		return user;
	}

	/**
	 * Generates a 32-byte cryptographically random token, URL-safe Base64 encoded.
	 */
	private String generateToken() {
		byte[] buf = new byte[TOKEN_BYTES];
		SECURE_RANDOM.nextBytes(buf);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
	}

	private String buildVerificationUrl(String token) {
		String base = emailProperties.getPublicBaseUrl();
		if (base.endsWith("/")) {
			base = base.substring(0, base.length() - 1);
		}
		return base + "/auth/verify-email/" + token;
	}

	private String renderHtml(String userName, String verifyUrl, int ttlMinutes) {
		return """
				<!DOCTYPE html>
				<html lang="ko">
				<body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; \
				background:#f5f5f5; padding:24px;">
				  <div style="max-width:560px; margin:0 auto; background:#ffffff; border-radius:12px; \
				padding:32px; border:1px solid #e5e7eb;">
				    <h1 style="font-size:22px; margin-top:0;">SettleTree 이메일 인증</h1>
				    <p>%s 님, 안녕하세요.</p>
				    <p>아래 버튼을 눌러 이메일 인증을 완료해주세요. 링크는 발송 시각 기준 <strong>%d분</strong> 동안 유효합니다.</p>
				    <p style="margin:32px 0;">
				      <a href="%s" style="display:inline-block; padding:12px 24px; background:#2D2D2D; \
				color:#ffffff; text-decoration:none; border-radius:8px; font-weight:600;">이메일 인증하기</a>
				    </p>
				    <p style="font-size:12px; color:#6b7280;">버튼이 동작하지 않으면 다음 주소를 브라우저에 직접 붙여넣으세요.<br/>%s</p>
				    <hr style="border:none; border-top:1px solid #e5e7eb; margin:24px 0;"/>
				    <p style="font-size:12px; color:#9ca3af;">본 메일을 요청하지 않으셨다면 무시하셔도 됩니다.</p>
				  </div>
				</body>
				</html>
				""".formatted(escape(userName), ttlMinutes, verifyUrl, verifyUrl);
	}

	private static String escape(String input) {
		if (input == null) {
			return "";
		}
		return input
				.replace("&", "&amp;")
				.replace("<", "&lt;")
				.replace(">", "&gt;")
				.replace("\"", "&quot;");
	}
}
