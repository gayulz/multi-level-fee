package com.example.settlement.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * [NEW] Activates configuration properties for email + rate-limit modules.
 *
 * <p>
 * {@link EmailProperties} and {@link RateLimitProperties} are kept as plain POJO
 * holders (no stereotype). Centralizing their registration here keeps the app
 * boot deterministic and makes it obvious where the binding lives.
 * </p>
 *
 * @author gayul.kim
 * @since 2026-04-28
 */
@Configuration
@EnableConfigurationProperties({ EmailProperties.class, RateLimitProperties.class })
public class EmailConfig {
}
