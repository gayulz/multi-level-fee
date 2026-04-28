package com.example.settlement.domain.repository;

import com.example.settlement.domain.entity.EmailSendLog;
import com.example.settlement.domain.entity.enums.EmailSendType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

/**
 * [NEW] Repository for {@link EmailSendLog}.
 *
 * <p>
 * Counting queries are scoped to a time window — used by RateLimitService
 * to compute current usage when the Caffeine cache misses.
 * </p>
 *
 * @author gayul.kim
 * @since 2026-04-28
 */
public interface EmailSendLogRepository extends JpaRepository<EmailSendLog, Long> {

	/**
	 * Counts successful sends to a given email within a time window.
	 *
	 * @param email     target email address
	 * @param sendType  send purpose
	 * @param threshold lower bound of the window (sentAt &gt;= threshold)
	 * @return number of successful sends since the threshold
	 */
	@Query("SELECT COUNT(l) FROM EmailSendLog l "
			+ "WHERE l.email = :email AND l.sendType = :sendType "
			+ "AND l.success = true AND l.sentAt >= :threshold")
	long countByEmailSince(@Param("email") String email,
						   @Param("sendType") EmailSendType sendType,
						   @Param("threshold") LocalDateTime threshold);

	/**
	 * Counts successful sends from a given IP within a time window.
	 *
	 * @param ipAddress client IP
	 * @param sendType  send purpose
	 * @param threshold lower bound of the window
	 * @return number of successful sends since the threshold
	 */
	@Query("SELECT COUNT(l) FROM EmailSendLog l "
			+ "WHERE l.ipAddress = :ipAddress AND l.sendType = :sendType "
			+ "AND l.success = true AND l.sentAt >= :threshold")
	long countByIpSince(@Param("ipAddress") String ipAddress,
						@Param("sendType") EmailSendType sendType,
						@Param("threshold") LocalDateTime threshold);
}
