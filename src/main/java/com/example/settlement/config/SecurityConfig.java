package com.example.settlement.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

/**
 * [NEW] Spring Security 설정 클래스.
 *
 * <p>
 * CSRF 정책:
 * - HTTP Session 기반 토큰 저장 (서버사이드 렌더링 환경에 적합)
 * - 모든 state-changing 요청(POST/PUT/DELETE/PATCH)에 토큰 검증 강제
 * - 이메일 인증 GET 링크(/auth/verify-email/**)는 idempotent하므로 CSRF 검증 대상 외
 * </p>
 *
 * @author gayul.kim
 * @since 2026-03-09
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CSRF: explicit session-based token repository (fail-closed by default)
                .csrf(csrf -> csrf
                        .csrfTokenRepository(new HttpSessionCsrfTokenRepository()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/auth/welcome", "/auth/login", "/auth/signup", "/auth/verify-email/**", "/auth/resend-verification", "/auth/demo-login")
                        .permitAll()
                        .requestMatchers("/css/**", "/js/**", "/fonts/**", "/images/**").permitAll()

                        .requestMatchers("/admin/users/**").hasRole("SUPER_ADMIN")
                        .requestMatchers("/nodes/create", "/nodes/edit/**", "/nodes/delete/**").hasRole("SUPER_ADMIN")

                        .requestMatchers("/admin/**", "/nodes/**").hasAnyRole("ADMIN", "SUPER_ADMIN")

                        .requestMatchers("/dashboard", "/settlement/**", "/settings/**").authenticated()

                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/auth/login")
                        .loginProcessingUrl("/auth/login")
                        .defaultSuccessUrl("/dashboard", true)
                        .failureUrl("/auth/login?error=true")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/auth/logout")
                        .logoutSuccessUrl("/auth/login?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll())
                .sessionManagement(session -> session
                        .maximumSessions(1)
                        .maxSessionsPreventsLogin(false))
                .exceptionHandling(exception -> exception
                        .accessDeniedPage("/error/403"));

        return http.build();
    }
}
