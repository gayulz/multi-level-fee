package com.example.settlement.web.security;

import com.example.settlement.domain.entity.User;
import com.example.settlement.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * [NEW] Spring Security UserDetailsService 구현체.
 *
 * @author gayul.kim
 * @since 2026-03-09
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Organization을 Fetch Join으로 함께 조회하여 로그인 시 N+1 쿼리 방지
        User user = userRepository.findByEmailWithOrganization(email)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + email));

        return new CustomUserDetails(user);
    }
}
