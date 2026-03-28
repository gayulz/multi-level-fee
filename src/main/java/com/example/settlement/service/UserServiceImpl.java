package com.example.settlement.service;

import com.example.settlement.domain.entity.Organization;
import com.example.settlement.domain.entity.User;
import com.example.settlement.domain.entity.enums.UserRole;
import com.example.settlement.domain.entity.enums.UserStatus;
import com.example.settlement.domain.repository.OrganizationRepository;
import com.example.settlement.domain.repository.UserRepository;
import com.example.settlement.dto.request.SignupRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * [NEW] 사용자 관리 서비스 구현체
 *
 * @author gayul.kim
 * @since 2026-03-09
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public User registerUser(SignupRequest request) {
        // 1. 이메일 중복 체크
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 이메일입니다");
        }

        // 2. 소속 조직 조회
        Organization org = organizationRepository.findById(request.getOrganizationId())
                .orElseThrow(() -> new IllegalArgumentException("조직을 찾을 수 없습니다"));

        // 3. 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        // 4. User 생성 (기본 권한: USER, 상태: PENDING)
        User user = User.createUser(
                request.getEmail(),
                encodedPassword,
                request.getName(),
                request.getPhone(),
                org,
                true // 개인정보 동의는 회원가입에서 기본적으로 동의한다고 가정하거나 추가 필드 필요
        );

        return userRepository.save(user);
    }

    @Override
    @PreAuthorize("hasRole('SUPER_ADMIN') or #id == principal.userId")
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: id=" + id));
    }

    @Override
    @PreAuthorize("hasRole('SUPER_ADMIN') or #email == principal.username")
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: email=" + email));
    }

    @Override
    @PreAuthorize("hasRole('SUPER_ADMIN') or (hasRole('ADMIN') and #orgId == principal.organization.orgId)")
    public List<User> getUsersByOrganization(Long orgId) {
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new IllegalArgumentException("조직을 찾을 수 없습니다: id=" + orgId));
        return userRepository.findByOrganization(org);
    }

    @Override
    @PreAuthorize("hasRole('SUPER_ADMIN') or (hasRole('ADMIN') and #orgId == principal.organization.orgId)")
    public List<User> getPendingUsers(Long orgId) {
        // QueryDSL 등 커스텀 메소드가 필요하지만 여기서는 스트림을 사용해 필터링하거나
        // JPA 메소드를 추가할 수도 있음. 이 예제에서는 기본 JPA 사용 시
        return userRepository.findByStatus(UserStatus.PENDING).stream()
                .filter(user -> user.getOrganization().getOrgId().equals(orgId))
                .toList();
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public void approveUser(Long userId, Long approverId) {
        User user = getUserById(userId);
        User approver = getUserById(approverId);

        user.approve(approver);
        // save는 영속성 컨텍스트에 의해 더티 체킹되지만 명시적으로 호출할 수도 있음
        userRepository.save(user);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public void rejectUser(Long userId, Long approverId, String reason) {
        User user = getUserById(userId);
        user.reject(reason);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public void changeUserRole(Long userId, UserRole role) {
        User user = getUserById(userId);
        user.changeRole(role);
        userRepository.save(user);
    }

    @Override
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public List<User> getAllPendingUsers() {
        return userRepository.findByStatus(UserStatus.PENDING);
    }

    @Override
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public long getActiveUsersCount() {
        return userRepository.findByStatus(UserStatus.APPROVED).size();
    }
}
