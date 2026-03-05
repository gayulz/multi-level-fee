package com.example.settlement.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * [NEW] QueryDSL 설정 클래스.
 *
 * JPAQueryFactory Bean을 등록하여 QueryDSL 기반 동적 쿼리를 지원합니다.
 *
 * @author gayul.kim
 * @since 2026-03-06
 */
@Configuration
public class QueryDslConfig {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * [NEW] JPAQueryFactory Bean 등록.
     *
     * QueryDSL을 사용하기 위한 팩토리를 생성합니다.
     *
     * @return JPAQueryFactory QueryDSL 쿼리 팩토리
     */
    @Bean
    public JPAQueryFactory jpaQueryFactory() {
        return new JPAQueryFactory(entityManager);
    }
}
