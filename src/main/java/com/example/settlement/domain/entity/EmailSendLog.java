package com.example.settlement.domain.entity;

import com.example.settlement.domain.entity.enums.EmailSendType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * [NEW] Email send audit log.
 *
 * <p>
 * Permanent record of every email-send attempt — used for:
 * - rate limiting (DB as source of truth, Caffeine cache as fast-path)
 * - security audit (abuse investigation)
 * - delivery analytics
 * </p>
 *
 * @author gayul.kim
 * @since 2026-04-28
 */
@Entity
@Table(
	name = "email_send_log",
	indexes = {
		@Index(name = "idx_email_send_log_email_sent_at", columnList = "email,sent_at"),
		@Index(name = "idx_email_send_log_ip_sent_at", columnList = "ip_address,sent_at")
	}
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailSendLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@Column(nullable = false, length = 255)
	private String email;

	/** Client IP address (X-Forwarded-For aware). Up to IPv6 length. */
	@Column(name = "ip_address", nullable = false, length = 64)
	private String ipAddress;

	@Enumerated(EnumType.STRING)
	@Column(name = "send_type", nullable = false, length = 30)
	private EmailSendType sendType;

	/** Whether the actual provider call (Resend) returned success. */
	@Column(nullable = false)
	private Boolean success;

	/** Provider-side error message — null on success. */
	@Column(name = "failure_reason", length = 500)
	private String failureReason;

	@Column(name = "sent_at", nullable = false, updatable = false)
	private LocalDateTime sentAt;

	/**
	 * [NEW] Factory for a successful send record.
	 */
	public static EmailSendLog success(String email, String ipAddress, EmailSendType sendType) {
		EmailSendLog log = new EmailSendLog();
		log.email = email;
		log.ipAddress = ipAddress;
		log.sendType = sendType;
		log.success = true;
		log.failureReason = null;
		return log;
	}

	/**
	 * [NEW] Factory for a failed send record.
	 */
	public static EmailSendLog failure(String email, String ipAddress, EmailSendType sendType, String reason) {
		EmailSendLog log = new EmailSendLog();
		log.email = email;
		log.ipAddress = ipAddress;
		log.sendType = sendType;
		log.success = false;
		log.failureReason = reason != null && reason.length() > 500 ? reason.substring(0, 500) : reason;
		return log;
	}

	@PrePersist
	private void onCreate() {
		this.sentAt = LocalDateTime.now();
	}
}
