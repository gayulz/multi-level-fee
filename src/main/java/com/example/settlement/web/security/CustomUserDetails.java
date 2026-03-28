package com.example.settlement.web.security;

import com.example.settlement.domain.entity.Organization;
import com.example.settlement.domain.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * [NEW] Spring Security UserDetails 구현체.
 *
 * @author gayul.kim
 * @since 2026-03-09
 */
public class CustomUserDetails implements UserDetails {

    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority(user.getRole().name()));
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return Boolean.TRUE.equals(user.getActive());
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.isLoginable();
    }

    public Long getUserId() {
        return user.getUserId();
    }

    public String getName() {
        return user.getName();
    }

    public User getUser() {
        return this.user;
    }

    public Organization getOrganization() {
        return user.getOrganization();
    }

    public Integer getOrgLevel() {
        return user.getOrganization() != null ? user.getOrganization().getLevel() : null;
    }

    public String getRole() {
        return user.getRole().name();
    }
}
