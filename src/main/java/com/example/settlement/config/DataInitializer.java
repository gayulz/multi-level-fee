package com.example.settlement.config;

import com.example.settlement.domain.entity.Organization;
import com.example.settlement.domain.entity.SettlementNode;
import com.example.settlement.domain.entity.SettlementRequest;
import com.example.settlement.domain.entity.User;
import com.example.settlement.domain.entity.enums.UserRole;
import com.example.settlement.domain.entity.enums.UserStatus;
import com.example.settlement.domain.repository.OrganizationRepository;
import com.example.settlement.domain.repository.SettlementNodeRepository;
import com.example.settlement.domain.repository.SettlementRequestRepository;
import com.example.settlement.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.math.BigDecimal;

/**
 * [NEW] 애플리케이션 기동 시 초기 데이터를 DB에 삽입하는 컴포넌트.
 *
 * <p>
 * H2 in-memory DB(create-drop) 환경에서 기동할 때마다 아래 데이터를 자동 생성합니다:
 * <ul>
 * <li>조직 계층: 본사 1개 → 지사 2개 → 대리점 2개</li>
 * <li>정산 노드: 각 조직별 1개씩</li>
 * <li>계정:
 * <ul>
 * <li>SUPER_ADMIN: admin@sattletree.io / admin1234</li>
 * <li>ADMIN (지사): branch@sattletree.io / admin1234</li>
 * <li>ADMIN (대리점): agency@sattletree.io / admin1234</li>
 * <li>USER (일반): user@sattletree.io / user1234</li>
 * </ul>
 * </li>
 * </ul>
 * </p>
 *
 * @author gayul.kim
 * @since 2026-03-09
 */
@Slf4j
@Component
@Profile("local") // [MIG] local 환경에서 기동 시 자동 데이터 생성 허용
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

        private final OrganizationRepository organizationRepository;
        private final SettlementNodeRepository settlementNodeRepository;
        private final SettlementRequestRepository settlementRequestRepository;
        private final UserRepository userRepository;
        private final PasswordEncoder passwordEncoder;

        @Override
        @Transactional
        public void run(ApplicationArguments args) {
                // 이미 데이터가 존재하면 중복 삽입 방지 로직 개선
                if (organizationRepository.count() > 0) {
                        log.info("[DataInitializer] 조직 데이터가 이미 존재합니다.");
                        long reqCount = settlementRequestRepository.count();
                        if (reqCount < 250) {
                                int needCount = (int) (250 - reqCount);
                                log.info("[DataInitializer] 정산 내역이 부족하여 추가 {}건을 생성합니다.", needCount);
                                generateFakeSettlementRequests(needCount);
                        } else {
                                log.info("[DataInitializer] 정산 내역도 충분하여 데이터 초기화를 건너뜁니다.");
                        }
                        return;
                }

                log.info("========================================");
                log.info("[DataInitializer] 초기 데이터 생성 시작");
                log.info("========================================");

                // ===========================
                // 1. 조직 계층 생성
                // ===========================
                Organization hq = Organization.createHeadquarters("SattleTree 본사", "HQ-001");
                organizationRepository.save(hq);

                Organization seoulBranch = Organization.createBranch("서울 지사", "BR-SEOUL", hq);
                Organization busanBranch = Organization.createBranch("부산 지사", "BR-BUSAN", hq);
                organizationRepository.save(seoulBranch);
                organizationRepository.save(busanBranch);

                Organization gangnamAgency = Organization.createAgency("강남 대리점", "AG-GANGNAM", seoulBranch);
                Organization jongnoAgency = Organization.createAgency("종로 대리점", "AG-JONGNO", seoulBranch);
                organizationRepository.save(gangnamAgency);
                organizationRepository.save(jongnoAgency);

                log.info("[DataInitializer] 조직 생성 완료: {}개", organizationRepository.count());

                // ===========================
                // 2. 정산 노드 생성 (각 조직별)
                // ===========================
                SettlementNode hqNode = createNode("본사 노드", hq, new BigDecimal("0.1000"), null);
                settlementNodeRepository.save(hqNode);

                SettlementNode seoulNode = createNode("서울지사 노드", seoulBranch, new BigDecimal("0.0500"), hqNode);
                settlementNodeRepository.save(seoulNode);

                SettlementNode busanNode = createNode("부산지사 노드", busanBranch, new BigDecimal("0.0500"), hqNode);
                settlementNodeRepository.save(busanNode);

                SettlementNode gangnamNode = createNode("강남대리점 노드", gangnamAgency, new BigDecimal("0.0300"), seoulNode);
                settlementNodeRepository.save(gangnamNode);

                SettlementNode jongnoNode = createNode("종로대리점 노드", jongnoAgency, new BigDecimal("0.0200"), seoulNode);
                settlementNodeRepository.save(jongnoNode);

                log.info("[DataInitializer] 정산 노드 생성 완료: {}개", settlementNodeRepository.count());

                // ===========================
                // 3. 계정 생성
                // ===========================
                String adminPassword = passwordEncoder.encode("admin1234");
                String userPassword = passwordEncoder.encode("user1234");

                // SUPER_ADMIN - 본사 소속
                User superAdmin = User.createSuperAdmin(
                                "admin@sattletree.io", adminPassword, "최고관리자", "010-0000-0000", hq);
                userRepository.save(superAdmin);

                // ADMIN (지사장) - 서울 지사 소속
                User branchAdmin = User.createSuperAdmin(
                                "branch@sattletree.io", adminPassword, "서울지사장", "010-1111-1111", seoulBranch);
                branchAdmin.changeRole(UserRole.ROLE_ADMIN);
                userRepository.save(branchAdmin);

                // ADMIN (대리점장) - 강남 대리점 소속
                User agencyAdmin = User.createSuperAdmin(
                                "agency@sattletree.io", adminPassword, "강남대리점장", "010-2222-2222", gangnamAgency);
                agencyAdmin.changeRole(UserRole.ROLE_ADMIN);
                userRepository.save(agencyAdmin);

                // USER (일반) - 종로 대리점 소속 → APPROVED + emailVerified=true
                User normalUser = User.createSuperAdmin(
                                "user@sattletree.io", userPassword, "일반사용자", "010-3333-3333", jongnoAgency);
                normalUser.changeRole(UserRole.ROLE_USER);
                userRepository.save(normalUser);

                // USER (대기 상태 샘플) - 강남 대리점 소속
                User pendingUser = User.createUser(
                                "pending@sattletree.io", userPassword, "가입대기사용자", "010-4444-4444", gangnamAgency, true);
                userRepository.save(pendingUser);

                log.info("[DataInitializer] 계정 생성 완료: {}개", userRepository.count());

                // ===========================
                // 4. 가상 데이터 확충 (직원 30명, 노드 5개)
                // ===========================
                log.info("[DataInitializer] 가상 데이터 추가 생성 시작...");

                // (1) 가상 하위 노드 5개 추가
                // 강남 하위 2개, 종로 하위 3개
                Organization seochoAgency = Organization.createAgency("서초 대리점", "AG-SEOCHO", gangnamAgency);
                Organization yeoksamAgency = Organization.createAgency("역삼 대리점", "AG-YEOKSAM", gangnamAgency);
                organizationRepository.save(seochoAgency);
                organizationRepository.save(yeoksamAgency);

                Organization gwanghwamunAgency = Organization.createAgency("광화문 대리점", "AG-GWANGHWAMUN", jongnoAgency);
                Organization euljiroAgency = Organization.createAgency("을지로 대리점", "AG-EULJIRO", jongnoAgency);
                Organization myeongdongAgency = Organization.createAgency("명동 대리점", "AG-MYEONGDONG", jongnoAgency);
                organizationRepository.save(gwanghwamunAgency);
                organizationRepository.save(euljiroAgency);
                organizationRepository.save(myeongdongAgency);

                settlementNodeRepository
                                .save(createNode("서초대리점 노드", seochoAgency, new BigDecimal("0.0200"), gangnamNode));
                settlementNodeRepository
                                .save(createNode("역삼대리점 노드", yeoksamAgency, new BigDecimal("0.0150"), gangnamNode));
                settlementNodeRepository
                                .save(createNode("광화문대리점 노드", gwanghwamunAgency, new BigDecimal("0.0100"), jongnoNode));
                settlementNodeRepository
                                .save(createNode("을지로대리점 노드", euljiroAgency, new BigDecimal("0.0120"), jongnoNode));
                settlementNodeRepository
                                .save(createNode("명동대리점 노드", myeongdongAgency, new BigDecimal("0.0110"), jongnoNode));

                // (2) 가상 직원 30명 추가 (강남/종로 대리점에 분산)
                Organization[] targetOrgs = { gangnamAgency, jongnoAgency, seochoAgency, yeoksamAgency,
                                gwanghwamunAgency };
                for (int i = 1; i <= 30; i++) {
                        String email = String.format("user%d@sattletree.io", i);
                        String name = String.format("테스트사용자%d", i);
                        Organization org = targetOrgs[i % targetOrgs.length];

                        User user = User.createSuperAdmin(email, userPassword, name,
                                        "010-9999-" + String.format("%04d", i), org);
                        user.changeRole(UserRole.ROLE_USER);
                        userRepository.save(user);
                }

                log.info("[DataInitializer] 가상 데이터 생성 완료: 노드 5개 추가, 직원 30명 추가");

                // ===========================
                // 5. 가상 정산 내역 50건 초기 생성 (없는 경우)
                // ===========================
                log.info("[DataInitializer] 초기 세팅 - 정산 내역 50건 생성 시작...");
                generateFakeSettlementRequests(50);
                log.info("========================================");
                log.info("[DataInitializer] 초기 데이터 생성 완료");
                log.info("===== 로그인 계정 정보 =====");
                log.info("  SUPER_ADMIN : admin@sattletree.io / admin1234");
                log.info("  가상 사용자 : user1@sattletree.io ~ user30@sattletree.io / user1234");
                log.info("========================================");
        }

        /**
         * [MIG] 지정된 건수만큼 가상 정산 내역을 생성합니다. (중복 없는 orderId 활용)
         *
         * @author gayul.kim
         * @param count 생성할 정산 내역 개수
         */
        private void generateFakeSettlementRequests(int count) {
                java.util.List<User> allUsers = userRepository.findAll();
                if (allUsers.isEmpty()) return;

                // 권한 기반 승인자 검색
                User superAdmin = allUsers.stream().filter(u -> u.getRole() == UserRole.ROLE_SUPER_ADMIN).findFirst().orElse(allUsers.get(0));
                User branchAdmin = allUsers.stream().filter(u -> u.getRole() == UserRole.ROLE_ADMIN).findFirst().orElse(allUsers.get(0));

                java.util.Random random = new java.util.Random();
                LocalDateTime now = LocalDateTime.now();

                // 트리의 최상단 루트 노드 스캔
                com.example.settlement.domain.entity.SettlementNode rootNode = settlementNodeRepository.findAll().stream()
                        .filter(node -> node.getParent() == null)
                        .findFirst()
                        .orElse(null);

                for (int i = 1; i <= count; i++) {
                        User requester = allUsers.get(random.nextInt(allUsers.size()));
                        BigDecimal amount = BigDecimal.valueOf(10000 + random.nextInt(990000));
                        // 나노초와 랜덤 인덱스로 무조건 고유한 orderId 발급 (중복방지)
                        String orderId = "ORD-" + now.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"))
                                        + "-" + ((System.nanoTime() / 1000) % 100000) + String.format("%03d", i);

                        SettlementRequest request = SettlementRequest.create(
                                        orderId,
                                        amount,
                                        "가상 정산 데이터 주입 #" + i,
                                        requester,
                                        requester.getOrganization(),
                                        rootNode);

                        // 상태 랜덤 설정 (PENDING, AGENCY_APPROVED, BRANCH_APPROVED, COMPLETED, REJECTED)
                        int statusSeed = random.nextInt(100);
                        if (statusSeed < 30) {
                                // PENDING (기본값)
                        } else if (statusSeed < 50) {
                                // AGENCY_APPROVED
                                request.approve(branchAdmin, "대리점 1차 자동 승인");
                        } else if (statusSeed < 60) {
                                // BRANCH_APPROVED
                                request.approve(branchAdmin, "대리점 승인");
                                request.approve(branchAdmin, "지사 2차 자동 승인");
                        } else if (statusSeed < 90) {
                                // COMPLETED
                                request.approve(branchAdmin, "대리점 승인");
                                request.approve(branchAdmin, "지사 승인");
                                request.approve(superAdmin, "최종 본사 자동 승인");
                                request.setSettlementAmounts(request.getAmount().multiply(new BigDecimal("0.05")),
                                                request.getAmount().multiply(new BigDecimal("0.95")));
                        } else {
                                // REJECTED
                                request.reject(branchAdmin, "자동 요건 미달로 반려");
                        }

                        // 작성일 랜덤 분산 (최근 14일)
                        try {
                                java.lang.reflect.Field createdAtField = SettlementRequest.class.getDeclaredField("createdAt");
                                createdAtField.setAccessible(true);
                                createdAtField.set(request, now.minusDays(random.nextInt(14)).minusHours(random.nextInt(24)));
                        } catch (Exception e) {
                                // 필드 설정 실패 시 기본 생성일 유지
                        }

                        settlementRequestRepository.save(request);
                }
                log.info("[DataInitializer] 가상 정산 내역 {}건 상세주입 완료", count);
        }

        /**
         * 정산 노드 생성 헬퍼.
         *
         * @author gayul.kim
         * @param name         노드명
         * @param organization 소속 조직
         * @param feeRate      수수료율
         * @param parent       부모 노드 (null이면 루트)
         * @return 생성된 SettlementNode
         */
        private SettlementNode createNode(String name, Organization organization,
                        BigDecimal feeRate, SettlementNode parent) {
                if (parent == null) {
                        return SettlementNode.createRoot(name, organization, feeRate);
                }
                return SettlementNode.createChild(name, organization, feeRate, parent);
        }
}
