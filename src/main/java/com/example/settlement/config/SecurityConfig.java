package com.example.settlement.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * [NEW] Spring Security 설정 클래스.
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
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/auth/welcome", "/auth/login", "/auth/signup", "/auth/verify-email/**")
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
