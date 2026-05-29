package com.example.settlement.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import java.util.Locale;

/**
 * [NEW] 다국어(i18n) 및 글로벌 로케일 WebMvc 설정 클래스
 *
 * @author gayul.kim
 * @since 2026-04-10
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(WebMvcConfig.class);

    /**
     * [NEW] 사용자의 언어 설정을 쿠키를 통해 저장하고 불러오는 Resolver
     * 기본값은 한국어(Locale.KOREAN)로 설정.
     *
     * @author gayul.kim
     * @return LocaleResolver
     */
    @Bean
    public LocaleResolver localeResolver() {
        CookieLocaleResolver resolver = new CookieLocaleResolver();
        resolver.setDefaultLocale(Locale.KOREAN);
        resolver.setCookieName("SettleTree_Lang");
        // 쿠키 수명: 30일
        resolver.setCookieMaxAge(30 * 24 * 60 * 60);
        return resolver;
    }

    /**
     * [NEW] HTTP 요청 파라미터(?lang=xx)를 감지하여 Locale을 변경하는 Interceptor
     *
     * 보안: 외부 봇이 lang 파라미터에 XSS/Path Traversal 페이로드를 주입하는 경우
     * 기본 LocaleChangeInterceptor 는 IllegalArgumentException 을 던져 500 에러로 이어진다.
     * 잘못된 값은 조용히 무시하고 기존 Locale 을 유지하도록 override 한다.
     *
     * @author gayul.kim
     * @return LocaleChangeInterceptor
     */
    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor() {
            @Override
            protected Locale parseLocaleValue(String localeValue) {
                try {
                    return super.parseLocaleValue(localeValue);
                } catch (IllegalArgumentException e) {
                    // Bot/scanner injected payload — keep current locale, do not raise 500
                    if (log.isDebugEnabled()) {
                        log.debug("Ignoring invalid 'lang' parameter value: {}", localeValue);
                    }
                    return null;
                }
            }
        };
        interceptor.setParamName("lang");
        return interceptor;
    }

    /**
     * [NEW] 인터셉터 레지스트리에 언어 변경 인터셉터 등록
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }
}
