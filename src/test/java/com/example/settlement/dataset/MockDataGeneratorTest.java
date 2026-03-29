package com.example.settlement.dataset;

import com.example.settlement.domain.entity.Organization;
import com.example.settlement.domain.entity.SettlementNode;
import com.example.settlement.domain.entity.SettlementRequest;
import com.example.settlement.domain.entity.User;
import com.example.settlement.domain.entity.enums.UserRole;
import com.example.settlement.domain.repository.OrganizationRepository;
import com.example.settlement.domain.repository.SettlementNodeRepository;
import com.example.settlement.domain.repository.SettlementRequestRepository;
import com.example.settlement.domain.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

/**
 * [MIG] 대규모 가상 데이터 1회성 적재(Seeding) 테스트 유틸리티.
 *
 * <p>
 * 로컬 개발 환경(PostgreSQL)에 대규모 샘플 데이터를 1회성으로 밀어넣기 위해 사용됩니다.
 * 평소 애플리케이션 빌드 시에는 수행되지 않으며, 사용자 수동 명령으로만 실행해야 합니다.
 * </p>
 *
 * @author gayul.kim
 * @since 2026-03-29
 */
@SpringBootTest
@ActiveProfiles("local") // PostgreSQL에 연결하기 위해 local 프로필 활성화
public class MockDataGeneratorTest {

    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private SettlementNodeRepository settlementNodeRepository;
    @Autowired private SettlementRequestRepository settlementRequestRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("대규모 가상 정산 데이터 세팅 (총 노드 10개, 사용자 285명, 정산 1,000건)")
    @Transactional
    @Commit // 테스트 종료 시 Rollback 대신 실제 DB 반영
    public void generateMassiveMockData() {
        if (organizationRepository.count() > 0) {
            System.out.println("=================================================");
            System.out.println(" [알림] 데이터가 이미 존재합니다.");
            System.out.println(" 새로 세팅하려면 DB(테이블) 데이터를 지우고 실행해주세요!");
            System.out.println(" (예: truncate table organization cascade; 등)");
            System.out.println("=================================================");
            return;
        }

        System.out.println("============= 가상 데이터 대량 생성 시작 =============");

        String adminPwd = passwordEncoder.encode("admin1234");
        String userPwd = passwordEncoder.encode("user1234");
        Random random = new Random();
        LocalDateTime now = LocalDateTime.now();

        // ==========================================
        // 1. 조직(Node) 구성 (본사 1, 지사 2, 대리점 7)
        // ==========================================
        Organization hq = organizationRepository.save(Organization.createHeadquarters("SattleTree 본사", "HQ-001"));
        Organization seoulBranch = organizationRepository.save(Organization.createBranch("서울 지사", "BR-SEOUL", hq));
        Organization busanBranch = organizationRepository.save(Organization.createBranch("부산 지사", "BR-BUSAN", hq));
        Organization gangnamAgency = organizationRepository.save(Organization.createAgency("강남 대리점", "AG-GANGNAM", seoulBranch));
        Organization jongnoAgency = organizationRepository.save(Organization.createAgency("종로 대리점", "AG-JONGNO", seoulBranch));
        Organization seochoAgency = organizationRepository.save(Organization.createAgency("서초 대리점", "AG-SEOCHO", gangnamAgency));
        Organization yeoksamAgency = organizationRepository.save(Organization.createAgency("역삼 대리점", "AG-YEOKSAM", gangnamAgency));
        Organization gwanghwamunAgency = organizationRepository.save(Organization.createAgency("광화문 대리점", "AG-GWANGHWAMUN", jongnoAgency));
        Organization euljiroAgency = organizationRepository.save(Organization.createAgency("을지로 대리점", "AG-EULJIRO", jongnoAgency));
        Organization myeongdongAgency = organizationRepository.save(Organization.createAgency("명동 대리점", "AG-MYEONGDONG", jongnoAgency));

        List<Organization> allOrgs = List.of(hq, seoulBranch, busanBranch, gangnamAgency, jongnoAgency, seochoAgency, yeoksamAgency, gwanghwamunAgency, euljiroAgency, myeongdongAgency);

        // ==========================================
        // 2. 정산 수수료 노드 생성
        // ==========================================
        SettlementNode hqNode = settlementNodeRepository.save(SettlementNode.createRoot("본사 정산계정", hq, new BigDecimal("0.1000")));
        SettlementNode seoulNode = settlementNodeRepository.save(SettlementNode.createChild("서울지사 정산계정", seoulBranch, new BigDecimal("0.0500"), hqNode));
        SettlementNode busanNode = settlementNodeRepository.save(SettlementNode.createChild("부산지사 정산계정", busanBranch, new BigDecimal("0.0500"), hqNode));
        SettlementNode gangnamNode = settlementNodeRepository.save(SettlementNode.createChild("강남대리점 정산계정", gangnamAgency, new BigDecimal("0.0300"), seoulNode));
        SettlementNode jongnoNode = settlementNodeRepository.save(SettlementNode.createChild("종로대리점 정산계정", jongnoAgency, new BigDecimal("0.0300"), seoulNode));
        settlementNodeRepository.save(SettlementNode.createChild("서초대리점 정산계정", seochoAgency, new BigDecimal("0.0200"), gangnamNode));
        settlementNodeRepository.save(SettlementNode.createChild("역삼대리점 정산계정", yeoksamAgency, new BigDecimal("0.0150"), gangnamNode));
        settlementNodeRepository.save(SettlementNode.createChild("광화문대리점 정산계정", gwanghwamunAgency, new BigDecimal("0.0100"), jongnoNode));
        settlementNodeRepository.save(SettlementNode.createChild("을지로대리점 정산계정", euljiroAgency, new BigDecimal("0.0120"), jongnoNode));
        settlementNodeRepository.save(SettlementNode.createChild("명동대리점 정산계정", myeongdongAgency, new BigDecimal("0.0110"), jongnoNode));

        // ==========================================
        // 3. 사용자 구성 (총 285명)
        // ==========================================
        // 본사 (20 일반, 5 관리자)
        generateUsers(hq, "hq", 20, 5, adminPwd, userPwd, UserRole.ROLE_SUPER_ADMIN);
        
        // 지사 (각 15 일반, 5 관리자)
        generateUsers(seoulBranch, "seoul", 15, 5, adminPwd, userPwd, UserRole.ROLE_ADMIN);
        generateUsers(busanBranch, "busan", 15, 5, adminPwd, userPwd, UserRole.ROLE_ADMIN);
        
        // 메인 대리점 (각 30 일반, 5 관리자)
        generateUsers(gangnamAgency, "gangnam", 30, 5, adminPwd, userPwd, UserRole.ROLE_ADMIN);
        generateUsers(jongnoAgency, "jongno", 30, 5, adminPwd, userPwd, UserRole.ROLE_ADMIN);
        
        // 하위 대리점 5곳 (각 30 일반, 0 관리자)
        generateUsers(seochoAgency, "seocho", 30, 0, adminPwd, userPwd, UserRole.ROLE_ADMIN);
        generateUsers(yeoksamAgency, "yeoksam", 30, 0, adminPwd, userPwd, UserRole.ROLE_ADMIN);
        generateUsers(gwanghwamunAgency, "gwanghwamun", 30, 0, adminPwd, userPwd, UserRole.ROLE_ADMIN);
        generateUsers(euljiroAgency, "euljiro", 30, 0, adminPwd, userPwd, UserRole.ROLE_ADMIN);
        generateUsers(myeongdongAgency, "myeongdong", 30, 0, adminPwd, userPwd, UserRole.ROLE_ADMIN);

        // ==========================================
        // 4. 노드별 100건, 총 1,000건 정산 내역 생성
        // ==========================================
        List<User> userPool = userRepository.findAll();
        // 승인 처리에 사용할 대표 관리자 (Fallback)
        User defaultAdmin = userPool.stream()
                .filter(u -> u.getRole() == UserRole.ROLE_SUPER_ADMIN)
                .findFirst().orElseThrow();

        for (Organization org : allOrgs) {
            // 해당 조직에 소속된 유저들만 필터링
            List<User> orgUsers = userPool.stream()
                    .filter(u -> u.getOrganization() != null && org.getOrgId().equals(u.getOrganization().getOrgId()))
                    .toList();

            if (orgUsers.isEmpty()) continue;

            for (int i = 1; i <= 100; i++) {
                User requester = orgUsers.get(random.nextInt(orgUsers.size()));
                BigDecimal amount = BigDecimal.valueOf(1000 + random.nextInt(999000));
                
                // UUID 대체용 유니크 주문번호
                String orderId = String.format("ORD-%s-%04d-%03d", 
                    now.format(java.time.format.DateTimeFormatter.ofPattern("yyMMddHHmmss")),
                    (System.nanoTime() / 1000) % 10000, i);

                SettlementRequest request = SettlementRequest.create(
                        orderId, 
                        amount, 
                        org.getOrgName() + " 자동발생 거래 #" + i, 
                        requester, 
                        org
                );

                applyRandomStatusAndDate(request, defaultAdmin, random, now);
                settlementRequestRepository.save(request);
            }
            System.out.println(" >> [" + org.getOrgName() + "] 정산내역 100건 생성 완료");
        }

        System.out.println("============= 가상 데이터 대량 생성(1,000건) 완료 =============");
    }

    /**
     * 특정 조직에 지정된 수만큼 사용자와 관리자를 생성합니다.
     */
    private void generateUsers(Organization org, String prefix, int userCount, int adminCount, String adminPwd, String userPwd, UserRole adminRole) {
        for (int i = 1; i <= adminCount; i++) {
            User admin = User.createSuperAdmin(prefix + "_admin" + i + "@sattletree.io", adminPwd, org.getOrgName() + " 관리자" + i, "010-0000-" + String.format("%04d", i), org);
            admin.changeRole(adminRole);
            userRepository.save(admin);
        }
        for (int i = 1; i <= userCount; i++) {
            // createSuperAdmin을 사용하면 STATUS=APPROVED, emailVerified=true 가 자동 적용됨
            User user = User.createSuperAdmin(prefix + "_user" + i + "@sattletree.io", userPwd, org.getOrgName() + " 일반" + i, "010-1111-" + String.format("%04d", i), org);
            user.changeRole(UserRole.ROLE_USER);
            userRepository.save(user);
        }
    }

    /**
     * 상태(PENDING/APPROVED/COMPLETED/REJECTED) 비율과 날짜를 흩뿌리는 메서드
     */
    private void applyRandomStatusAndDate(SettlementRequest request, User admin, Random random, LocalDateTime now) {
        int seed = random.nextInt(100);
        // 상태 랜덤 분배
        if (seed < 40) {
            // COMPLETED (완료)
            request.approve(admin, "1차 대리점 시스템 승인");
            request.approve(admin, "최종 본사 자동 정산 완료");
            request.setSettlementAmounts(request.getAmount().multiply(new BigDecimal("0.05")), request.getAmount().multiply(new BigDecimal("0.95")));
        } else if (seed < 55) {
            request.approve(admin, "AGENCY_APPROVED 승인");
        } else if (seed < 70) {
            request.approve(admin, "대리점 승인");
            request.approve(admin, "BRANCH_APPROVED 승인");
        } else if (seed < 80) {
            request.reject(admin, "증빙 누락 반려 처리");
        }
        // 나머지 < 100 은 PENDING

        // 작성일 랜덤 분배 (최근 30일 이내)
        try {
            java.lang.reflect.Field field = SettlementRequest.class.getDeclaredField("createdAt");
            field.setAccessible(true);
            field.set(request, now.minusDays(random.nextInt(30)).minusHours(random.nextInt(24)).minusMinutes(random.nextInt(60)));
        } catch (Exception ignored) {
            // 기본값 유지
        }
    }
}
