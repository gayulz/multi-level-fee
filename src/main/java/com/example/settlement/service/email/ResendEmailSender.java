package com.example.settlement.service.email;

import com.example.settlement.config.EmailProperties;
import com.example.settlement.exception.EmailSendException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * [NEW] Resend HTTP API email sender.
 *
 * <p>
 * Activated when {@code app.email.provider=resend}. Calls
 * {@code POST https://api.resend.com/emails} with a Bearer API key.
 * </p>
 *
 * <p>
 * Errors from the provider are wrapped in {@link EmailSendException} so callers can
 * apply uniform retry / audit handling regardless of the underlying provider.
 * </p>
 *
 * @author gayul.kim
 * @since 2026-04-28
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.email.provider", havingValue = "resend")
public class ResendEmailSender implements EmailSender {

	private final EmailProperties properties;
	private final RestTemplate restTemplate;

	public ResendEmailSender(EmailProperties properties) {
		this.properties = properties;
		this.restTemplate = new RestTemplate();
	}

	@Override
	public void send(EmailMessage message) {
		String apiKey = properties.getResend().getApiKey();
		if (apiKey == null || apiKey.isBlank()) {
			throw new EmailSendException("Resend API key is not configured (RESEND_API_KEY)");
		}

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setBearerAuth(apiKey);

		Map<String, Object> body = Map.of(
				"from", properties.getFrom(),
				"to", new String[] { message.to() },
				"subject", message.subject(),
				"html", message.htmlBody()
		);

		HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

		try {
			ResponseEntity<String> response = restTemplate.postForEntity(
					properties.getResend().getApiUrl(), request, String.class);
			HttpStatusCode status = response.getStatusCode();
			if (!status.is2xxSuccessful()) {
				throw new EmailSendException(
						"Resend returned non-2xx status: " + status.value() + ", body=" + response.getBody());
			}
			log.info("[EMAIL][RESEND] sent to={}, subject={}, status={}",
					message.to(), message.subject(), status.value());
		} catch (HttpStatusCodeException e) {
			// Resend returns structured error JSON — capture for audit/log
			throw new EmailSendException(
					"Resend HTTP error: status=" + e.getStatusCode().value() + ", body=" + e.getResponseBodyAsString(), e);
		} catch (RestClientException e) {
			throw new EmailSendException("Resend network error: " + e.getMessage(), e);
		}
	}

	@Override
	public String providerName() {
		return "resend";
	}
}
