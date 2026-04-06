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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@SpringBootTest
public class MockDataGeneratorTest {

    @Autowired
    private OrganizationRepository organizationRepository;
    @Autowired
    private SettlementNodeRepository settlementNodeRepository;
    @Autowired
    private SettlementRequestRepository settlementRequestRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private jakarta.persistence.EntityManager em;

    @Test
    @DisplayName("DB 영구 저장을 위한 1,000건 대규모 가상 데이터 자동 생성 테스트")
    @Transactional
    @Commit
    public void generateMassiveMockData() {
        System.out.println("========================================");
        System.out.println("[MockDataGeneratorTest] 대규모 가상 데이터 생성 시작");
        
        // 데이터가 이미 있으면 중복을 막기 위해 기존 테이블 데이터 모두 삭제 (초기화)
        if (organizationRepository.count() > 0) {
            System.out.println("[MockDataGeneratorTest] 기존 데이터를 초기화(TRUNCATE) 합니다...");
            em.createNativeQuery("TRUNCATE TABLE settlement_request CASCADE").executeUpdate();
            em.createNativeQuery("TRUNCATE TABLE settlement_node CASCADE").executeUpdate();
            em.createNativeQuery("TRUNCATE TABLE users CASCADE").executeUpdate();
            em.createNativeQuery("TRUNCATE TABLE organization CASCADE").executeUpdate();
        }

        // 1. 조직 계층 생성 (총 10개)
        Organization hq = Organization.createHeadquarters("본사", "HQ-001");
        organizationRepository.save(hq);

        Organization seoul = Organization.createBranch("서울지사", "BR-SEOUL", hq);
        Organization busan = Organization.createBranch("부산지사", "BR-BUSAN", hq);
        organizationRepository.saveAll(Arrays.asList(seoul, busan));

        Organization gangnam = Organization.createAgency("강남대리점", "AG-GANGNAM", seoul);
        Organization jongno = Organization.createAgency("종로대리점", "AG-JONGNO", seoul);
        Organization seocho = Organization.createAgency("서초대리점", "AG-SEOCHO", seoul);
        Organization yeouido = Organization.createAgency("여의도대리점", "AG-YEOUIDO", seoul);

        Organization haeundae = Organization.createAgency("해운대대리점", "AG-HAEUNDAE", busan);
        Organization seomyeon = Organization.createAgency("서면대리점", "AG-SEOMYEON", busan);
        Organization nampo = Organization.createAgency("남포대리점", "AG-NAMPO", busan);

        organizationRepository.saveAll(Arrays.asList(gangnam, jongno, seocho, yeouido, haeundae, seomyeon, nampo));

        // 2. 정산 노드 생성
        SettlementNode hqNode = settlementNodeRepository.save(SettlementNode.createRoot("본사 노드", hq, new BigDecimal("0.1000")));
        SettlementNode seoulNode = settlementNodeRepository.save(SettlementNode.createChild("서울지사 노드", seoul, new BigDecimal("0.0500"), hqNode));
        SettlementNode busanNode = settlementNodeRepository.save(SettlementNode.createChild("부산지사 노드", busan, new BigDecimal("0.0500"), hqNode));
        
        SettlementNode gangnamNode = settlementNodeRepository.save(SettlementNode.createChild("강남대리점 노드", gangnam, new BigDecimal("0.0300"), seoulNode));
        SettlementNode jongnoNode = settlementNodeRepository.save(SettlementNode.createChild("종로대리점 노드", jongno, new BigDecimal("0.0200"), seoulNode));
        SettlementNode seochoNode = settlementNodeRepository.save(SettlementNode.createChild("서초대리점 노드", seocho, new BigDecimal("0.0250"), seoulNode));
        SettlementNode yeouidoNode = settlementNodeRepository.save(SettlementNode.createChild("여의도대리점 노드", yeouido, new BigDecimal("0.0150"), seoulNode));

        SettlementNode haeundaeNode = settlementNodeRepository.save(SettlementNode.createChild("해운대대리점 노드", haeundae, new BigDecimal("0.0250"), busanNode));
        SettlementNode seomyeonNode = settlementNodeRepository.save(SettlementNode.createChild("서면대리점 노드", seomyeon, new BigDecimal("0.0300"), busanNode));
        SettlementNode nampoNode = settlementNodeRepository.save(SettlementNode.createChild("남포대리점 노드", nampo, new BigDecimal("0.0200"), busanNode));

        // 3. 사용자 생성
        String pass = passwordEncoder.encode("admin1234");
        User superAdmin = User.createSuperAdmin("admin@settletree.io", pass, "운영관리자", "010-0000-0000", hq);
        superAdmin.changeRole(UserRole.ROLE_ADMIN);
        superAdmin = userRepository.save(superAdmin);
        
        List<Organization> orgs = Arrays.asList(hq, seoul, busan, gangnam, jongno, seocho, yeouido, haeundae, seomyeon, nampo);
        List<User> allUsers = new ArrayList<>();
        allUsers.add(superAdmin);

        for (Organization org : orgs) {
            String orgPrefix = "org" + org.getOrgId();
            
            int adminCount = (org == hq || org == seoul || org == busan || org == gangnam || org == jongno) ? 5 : 1;
            // 본사는 일반 20명, 지사와 대리점은 모두 일반 30명
            int userCount = (org == hq) ? 20 : 30;

            for(int i=1; i<=adminCount; i++) {
                User admin = User.createSuperAdmin(
                        String.format("admin%d@%s.settletree.io", i, orgPrefix),
                        pass, org.getOrgName() + "관리자" + i, "010-9999-000" + i, org);
                admin.changeRole(UserRole.ROLE_ADMIN);
                allUsers.add(userRepository.save(admin));
            }

            for(int i=1; i<=userCount; i++) {
                User normalUser = User.createSuperAdmin(
                        String.format("user%d@%s.settletree.io", i, orgPrefix),
                        pass, org.getOrgName() + "사용자" + i, "010-8888-00" + i, org);
                normalUser.changeRole(UserRole.ROLE_USER);
                allUsers.add(userRepository.save(normalUser));
            }
        }

        System.out.println("[MockDataGeneratorTest] 노드 및 사용자 세팅 완료. 회원 수: " + allUsers.size());
        
        // 4. 정산 요청 1,000건 (각 조직별 100건)
        Random random = new Random();
        LocalDateTime now = LocalDateTime.now();
        int reqCount = 0;

        for (Organization org : orgs) {
            List<User> orgUsers = allUsers.stream().filter(u -> u.getOrganization().getOrgId().equals(org.getOrgId())).toList();

            for (int i=1; i<=100; i++) {
                User requester = orgUsers.get(random.nextInt(orgUsers.size()));
                BigDecimal amount = BigDecimal.valueOf(10000 + random.nextInt(490000));
                String orderId = "ORD-" + org.getOrgId() + "-" + String.format("%04d", i);

                SettlementRequest request = SettlementRequest.create(
                        orderId, amount, org.getOrgName() + " 가상 정산 #" + i, requester, org);

                // 상태 랜덤화 및 승인
                int s = random.nextInt(100);
                try {
                    if (s < 20) {
                        // PENDING 상태 그대로
                    } else if (s < 40) {
                        request.approve(superAdmin, "대리점 1차 승인");
                    } else if (s < 60) {
                        request.approve(superAdmin, "대리점 1차 승인");
                        request.approve(superAdmin, "지사 승인");
                    } else if (s < 90) {
                        request.approve(superAdmin, "대리점 승인");
                        request.approve(superAdmin, "지사 승인");
                        request.approve(superAdmin, "본사 최종 승인");
                        request.setSettlementAmounts(request.getAmount().multiply(new BigDecimal("0.05")),
                                                     request.getAmount().multiply(new BigDecimal("0.95")));
                    } else {
                        request.reject(superAdmin, "서류 미비 또는 내부 사유거절");
                    }
                } catch (IllegalStateException e) {
                    // 조직 계층(본사 등)에 따라 1번 만에 COMPLETED 되고 두 번째 승인 시도시 에러가 나는 경우 무시
                }

                try {
                    java.lang.reflect.Field f = SettlementRequest.class.getDeclaredField("createdAt");
                    f.setAccessible(true);
                    f.set(request, now.minusDays(random.nextInt(30)).minusHours(random.nextInt(24)));
                } catch(Exception ignored) {}

                settlementRequestRepository.save(request);
                reqCount++;
            }
        }

        System.out.println("[MockDataGeneratorTest] 정산요청 총 " + reqCount + "건 적재 완료.");
        System.out.println("========================================");
    }
}