package com.example.settlement.domain.repository;

import com.example.settlement.domain.entity.User;
import com.example.settlement.domain.entity.enums.UserRole;
import com.example.settlement.domain.entity.enums.UserStatus;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.example.settlement.domain.entity.QUser.user;

/**
 * [NEW] 사용자 QueryDSL Custom Repository 구현체.
 *
 * @author gayul.kim
 * @since 2026-03-07
 */
@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    /**
     * {@inheritDoc}
     *
     * @author gayul.kim
     */
    @Override
    public List<User> findByOrganizationAndRole(Long orgId, UserRole role) {
        return queryFactory
                .selectFrom(user)
                .where(
                        user.organization.orgId.eq(orgId),
                        user.role.eq(role))
                .fetch();
    }

    /**
     * {@inheritDoc}
     *
     * @author gayul.kim
     */
    @Override
    public List<User> findPendingByOrganization(Long orgId) {
        return queryFactory
                .selectFrom(user)
                .where(
                        user.organization.orgId.eq(orgId),
                        user.status.eq(UserStatus.PENDING))
                .orderBy(user.requestedAt.asc())
                .fetch();
    }

    /**
     * {@inheritDoc}
     *
     * @author gayul.kim
     */
    @Override
    public List<User> findByOrganizationIn(List<Long> orgIds) {
        return queryFactory
                .selectFrom(user)
                .where(user.organization.orgId.in(orgIds))
                .fetch();
    }
}
