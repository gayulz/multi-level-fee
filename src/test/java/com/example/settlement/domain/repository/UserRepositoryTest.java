package com.example.settlement.domain.repository;

import com.example.settlement.domain.entity.Organization;
import com.example.settlement.domain.entity.User;
import com.example.settlement.domain.entity.enums.UserStatus;
import com.example.settlement.messaging.SettlementMessageConsumer;
import com.example.settlement.messaging.SettlementMessageProducer;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * [NEW] 사용자 Repository 통합 테스트.
 *
 * H2 인메모리 DB를 사용하여 Repository의 기능을 테스트합니다.
 *
 * @author gayul.kim
 * @since 2026-03-28
 */
@SpringBootTest
@Transactional
@ActiveProfiles("test")
class UserRepositoryTest {

    @MockBean
    private RabbitTemplate rabbitTemplate;
    
    @MockBean
    private SettlementMessageConsumer settlementMessageConsumer;
    
    @MockBean
    private SettlementMessageProducer settlementMessageProducer;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository orgRepository;

    @Autowired
    private EntityManager em;

    private Organization commonOrg;

    @BeforeEach
    void setUp() {
        // 공통 테스트 픽스처: 기본 조직 생성
        commonOrg = Organization.createHeadquarters("테스트 본사", "TEST-HQ-01");
        orgRepository.save(commonOrg);
    }

    @Test
    @DisplayName("이메일로 사용자를 단건 조회할 수 있어야 함")
    void 이메일_사용자_조회() {
        // Given
        User user = User.createUser("testuser@email.com", "encodedPassword123", "테스트유저", "010-1234-5678", commonOrg, true);
        userRepository.save(user);

        em.flush();
        em.clear();

        // When
        Optional<User> found = userRepository.findByEmail("testuser@email.com");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("테스트유저");
        assertThat(found.get().getOrganization().getOrgCode()).isEqualTo("TEST-HQ-01");
    }

    @Test
    @DisplayName("특정 조직에 소속된 사용자 목록을 조회할 수 있어야 함")
    void 특정_조직_사용자_조회() {
        // Given
        Organization otherOrg = Organization.createBranch("테스트 지사", "TEST-BR-01", commonOrg);
        orgRepository.save(otherOrg);

        User user1 = User.createUser("user1@email.com", "pw1", "유저1", "010-1111-2222", commonOrg, true);
        User user2 = User.createUser("user2@email.com", "pw2", "유저2", "010-3333-4444", commonOrg, true);
        User user3 = User.createUser("user3@email.com", "pw3", "유저3", "010-5555-6666", otherOrg, true);
        
        userRepository.saveAll(List.of(user1, user2, user3));

        em.flush();
        em.clear();

        // When
        List<User> hqUsers = userRepository.findByOrganization(commonOrg);
        List<User> branchUsers = userRepository.findByOrganization(otherOrg);

        // Then
        assertThat(hqUsers).hasSize(2)
            .extracting("email")
            .containsExactlyInAnyOrder("user1@email.com", "user2@email.com");
        
        assertThat(branchUsers).hasSize(1);
        assertThat(branchUsers.get(0).getEmail()).isEqualTo("user3@email.com");
    }

    @Test
    @DisplayName("가입 상태별로 사용자 목록을 조회할 수 있어야 함")
    void 가입_상태별_사용자_조회() {
        // Given
        User pendingUser = User.createUser("pending@email.com", "pw", "대기유저", "010-1111-1111", commonOrg, true);
        User approvedAdmin = User.createSuperAdmin("admin@email.com", "pw", "슈퍼어드민", "010-9999-9999", commonOrg);
        
        userRepository.saveAll(List.of(pendingUser, approvedAdmin));

        em.flush();
        em.clear();

        // When
        List<User> pendingUsers = userRepository.findByStatus(UserStatus.PENDING);
        List<User> approvedUsers = userRepository.findByStatus(UserStatus.APPROVED);

        // Then
        assertThat(pendingUsers).hasSize(1);
        assertThat(pendingUsers.get(0).getEmail()).isEqualTo("pending@email.com");

        assertThat(approvedUsers).hasSize(1);
        assertThat(approvedUsers.get(0).getEmail()).isEqualTo("admin@email.com");
    }

    @Test
    @DisplayName("중복된 이메일로 사용자를 저장하려고 하면 예외가 발생해야 함 (유니크 제약조건)")
    void 중복_이메일_저장_실패() {
        // Given
        String duplicateEmail = "duplicate@email.com";
        User user1 = User.createUser(duplicateEmail, "pw1", "유저1", "010-1111-1111", commonOrg, true);
        User user2 = User.createUser(duplicateEmail, "pw2", "유저2", "010-2222-2222", commonOrg, true);

        // When & Then
        userRepository.save(user1);
        assertThrows(DataIntegrityViolationException.class, () -> {
            userRepository.save(user2);
            em.flush(); // 실제 제약조건 검증을 위해 flush 필수
        });
    }

    @Test
    @DisplayName("이메일 존재 여부를 정상적으로 확인할 수 있어야 함")
    void 이메일_존재여부_검증() {
        // Given
        User user = User.createUser("exist@email.com", "pw", "유저", "010-1111-1111", commonOrg, true);
        userRepository.save(user);

        em.flush();
        em.clear();

        // When
        boolean exists = userRepository.existsByEmail("exist@email.com");
        boolean notExists = userRepository.existsByEmail("nothere@email.com");

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }
}
