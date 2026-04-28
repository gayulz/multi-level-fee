package com.example.settlement.service.email;

/**
 * [NEW] Provider-agnostic email payload.
 *
 * @param to       recipient email
 * @param subject  email subject
 * @param htmlBody HTML body (UTF-8)
 *
 * @author gayul.kim
 * @since 2026-04-28
 */
public record EmailMessage(String to, String subject, String htmlBody) {
}
