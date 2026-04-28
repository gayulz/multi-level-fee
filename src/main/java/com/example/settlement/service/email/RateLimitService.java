package com.example.settlement.service.email;

import com.example.settlement.config.RateLimitProperties;
import com.example.settlement.domain.entity.enums.EmailSendType;
import com.example.settlement.domain.repository.EmailSendLogRepository;
import com.example.settlement.exception.RateLimitExceededException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * [NEW] Rate limit guard for email sending.
 *
 * <p>
 * Two-layer counting:
 * - Caffeine cache (fast path) holds an approximate counter that auto-expires
 *   after the rate-limit window. Every successful send increments the cache.
 * - DB (source of truth) is consulted only on cache miss to seed the counter
 *   with actual usage so the rate-limit survives application restarts.
 * </p>
 *
 * <p>
 * Single-instance assumption: with multiple app instances each cache is local.
 * The DB count keeps things consistent enough — a brief over-count window is
 * accepted as a trade-off for not requiring Redis.
 * </p>
 *
 * @author gayul.kim
 * @since 2026-04-28
 */
@Slf4j
@Service
public class RateLimitService {

	private static final String K_EMAIL_HOUR = "email:hour:";
	private static final String K_EMAIL_DAY = "email:day:";
	private static final String K_IP_HOUR = "ip:hour:";
	private static final String K_IP_DAY = "ip:day:";

	private final RateLimitProperties properties;
	private final EmailSendLogRepository logRepository;

	/** Hourly counters — entries auto-expire 1 hour after creation. */
	private final Cache<String, AtomicLong> hourlyCounters = Caffeine.newBuilder()
			.expireAfterWrite(1, TimeUnit.HOURS)
			.maximumSize(50_000)
			.build();

	/** Daily counters — entries auto-expire 24 hours after creation. */
	private final Cache<String, AtomicLong> dailyCounters = Caffeine.newBuilder()
			.expireAfterWrite(24, TimeUnit.HOURS)
			.maximumSize(50_000)
			.build();

	public RateLimitService(RateLimitProperties properties, EmailSendLogRepository logRepository) {
		this.properties = properties;
		this.logRepository = logRepository;
	}

	/**
	 * Checks all four rate-limit dimensions before an email send attempt.
	 * Throws {@link RateLimitExceededException} if any limit is exceeded.
	 *
	 * @param email     target email
	 * @param ipAddress client IP
	 * @param sendType  send purpose
	 */
	public void checkBeforeSend(String email, String ipAddress, EmailSendType sendType) {
		LocalDateTime now = LocalDateTime.now();

		long emailHour = currentCount(K_EMAIL_HOUR + sendType + ":" + email,
				hourlyCounters,
				() -> logRepository.countByEmailSince(email, sendType, now.minusHours(1)));
		if (emailHour >= properties.getEmailHourlyLimit()) {
			throw new RateLimitExceededException("EMAIL_HOURLY", 3600,
					"이메일별 시간당 발송 한도(" + properties.getEmailHourlyLimit() + "회)를 초과했습니다. 1시간 후 다시 시도해주세요.");
		}

		long emailDay = currentCount(K_EMAIL_DAY + sendType + ":" + email,
				dailyCounters,
				() -> logRepository.countByEmailSince(email, sendType, now.minusHours(24)));
		if (emailDay >= properties.getEmailDailyLimit()) {
			throw new RateLimitExceededException("EMAIL_DAILY", 86400,
					"이메일별 일일 발송 한도(" + properties.getEmailDailyLimit() + "회)를 초과했습니다. 24시간 후 다시 시도해주세요.");
		}

		long ipHour = currentCount(K_IP_HOUR + sendType + ":" + ipAddress,
				hourlyCounters,
				() -> logRepository.countByIpSince(ipAddress, sendType, now.minusHours(1)));
		if (ipHour >= properties.getIpHourlyLimit()) {
			throw new RateLimitExceededException("IP_HOURLY", 3600,
					"IP별 시간당 발송 한도를 초과했습니다. 1시간 후 다시 시도해주세요.");
		}

		long ipDay = currentCount(K_IP_DAY + sendType + ":" + ipAddress,
				dailyCounters,
				() -> logRepository.countByIpSince(ipAddress, sendType, now.minusHours(24)));
		if (ipDay >= properties.getIpDailyLimit()) {
			throw new RateLimitExceededException("IP_DAILY", 86400,
					"IP별 일일 발송 한도를 초과했습니다. 24시간 후 다시 시도해주세요.");
		}
	}

	/**
	 * Records a successful send by incrementing all four counters.
	 * Called by EmailVerificationService AFTER the send succeeds and the audit log is persisted.
	 */
	public void recordSuccessfulSend(String email, String ipAddress, EmailSendType sendType) {
		incrementCounter(K_EMAIL_HOUR + sendType + ":" + email, hourlyCounters);
		incrementCounter(K_EMAIL_DAY + sendType + ":" + email, dailyCounters);
		incrementCounter(K_IP_HOUR + sendType + ":" + ipAddress, hourlyCounters);
		incrementCounter(K_IP_DAY + sendType + ":" + ipAddress, dailyCounters);
	}

	private long currentCount(String key, Cache<String, AtomicLong> cache, java.util.function.LongSupplier dbLoader) {
		AtomicLong counter = cache.get(key, k -> new AtomicLong(dbLoader.getAsLong()));
		return counter.get();
	}

	private void incrementCounter(String key, Cache<String, AtomicLong> cache) {
		AtomicLong counter = cache.get(key, k -> new AtomicLong(0L));
		counter.incrementAndGet();
	}

	/**
	 * Test-only utility: clears all counters. Production code must not call this.
	 */
	void clearAllForTesting() {
		hourlyCounters.invalidateAll();
		dailyCounters.invalidateAll();
	}

	/**
	 * Helper for tests/admin to query a counter — not used in production paths.
	 */
	long peek(String emailOrIp, EmailSendType sendType, String dimension) {
		String key = switch (dimension) {
			case "EMAIL_HOUR" -> K_EMAIL_HOUR + sendType + ":" + emailOrIp;
			case "EMAIL_DAY" -> K_EMAIL_DAY + sendType + ":" + emailOrIp;
			case "IP_HOUR" -> K_IP_HOUR + sendType + ":" + emailOrIp;
			case "IP_DAY" -> K_IP_DAY + sendType + ":" + emailOrIp;
			default -> throw new IllegalArgumentException("Unknown dimension: " + dimension);
		};
		Cache<String, AtomicLong> cache = (dimension.endsWith("HOUR")) ? hourlyCounters : dailyCounters;
		AtomicLong counter = cache.getIfPresent(key);
		return counter == null ? 0L : counter.get();
	}

	@SuppressWarnings("unused")
	private static Duration windowFor(String dimension) {
		return dimension.endsWith("HOUR") ? Duration.ofHours(1) : Duration.ofHours(24);
	}
}
