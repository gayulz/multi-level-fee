package com.example.settlement.demo;

import com.example.settlement.domain.entity.Organization;
import com.example.settlement.domain.entity.SettlementNode;
import com.example.settlement.domain.entity.User;
import com.example.settlement.domain.entity.enums.UserRole;
import com.example.settlement.domain.repository.OrganizationRepository;
import com.example.settlement.domain.repository.SettlementNodeRepository;
import com.example.settlement.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static com.example.settlement.demo.DemoConstants.*;

/**
 * [NEW] 데모 모드 데이터 시더.
 *
 * <p>
 * app.demo.enabled=true 일 때 앱 기동 시 1회 실행되어 데모 전용 조직 트리와
 * 4개 역할별 데모 계정을 생성합니다. 기존 일반 사용자 데이터와 격리하기 위해
 * DEMO- prefix가 붙은 조직 코드를 사용합니다.
 * </p>
 *
 * <p>
 * 멱등성 보장: DEMO_HQ_CODE 조직이 이미 존재하면 스킵합니다. DataInitializer
 * 이후에 실행되도록 @Order로 보장합니다(DataInitializer 기본 순서보다 큼).
 * </p>
 *
 * @author gayul.kim
 * @since 2026-05-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(100)
public class DemoDataSeeder implements ApplicationRunner {

	private final OrganizationRepository organizationRepository;
	private final SettlementNodeRepository settlementNodeRepository;
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final DemoProperties demoProperties;
	private final DemoSettlementSeeder demoSettlementSeeder;

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (!demoProperties.isEnabled()) {
			log.info("[DemoDataSeeder] app.demo.enabled=false - skip demo seed");
			return;
		}

		if (organizationRepository.findByOrgCode(DEMO_HQ_CODE).isPresent()) {
			log.info("[DemoDataSeeder] Demo HQ already exists - skip seed");
			return;
		}

		log.info("[DemoDataSeeder] ======== Demo data seeding started ========");
		long start = System.currentTimeMillis();

		// 1) Create demo org tree and settlement nodes
		Organization hq = createDemoOrgTree();

		// 2) Create 4 demo accounts (one per role)
		createDemoAccounts(hq);

		// 3) Seed demo settlement requests (delegated to keep file size small)
		demoSettlementSeeder.seedSettlements();

		long elapsed = System.currentTimeMillis() - start;
		log.info("[DemoDataSeeder] ======== Demo data seeding finished ({}ms) ========", elapsed);
	}

	/**
	 * [NEW] Build the demo org tree: HQ -> Branch -> {AgencyA, AgencyB}.
	 *
	 * @return demo HQ organization (root)
	 * @author gayul.kim
	 */
	private Organization createDemoOrgTree() {
		Organization hq = Organization.createHeadquarters(DEMO_HQ_NAME, DEMO_HQ_CODE);
		organizationRepository.save(hq);
		SettlementNode hqNode = SettlementNode.createRoot(DEMO_HQ_NAME, hq, new BigDecimal("0.1000"));
		settlementNodeRepository.save(hqNode);

		Organization branch = Organization.createBranch(DEMO_BRANCH_NAME, DEMO_BRANCH_CODE, hq);
		organizationRepository.save(branch);
		SettlementNode branchNode = SettlementNode.createChild(DEMO_BRANCH_NAME, branch,
				new BigDecimal("0.0500"), hqNode);
		settlementNodeRepository.save(branchNode);

		Organization agencyA = Organization.createAgency(DEMO_AGENCY_A_NAME, DEMO_AGENCY_A_CODE, branch);
		organizationRepository.save(agencyA);
		SettlementNode agencyANode = SettlementNode.createChild(DEMO_AGENCY_A_NAME, agencyA,
				new BigDecimal("0.0200"), branchNode);
		settlementNodeRepository.save(agencyANode);

		Organization agencyB = Organization.createAgency(DEMO_AGENCY_B_NAME, DEMO_AGENCY_B_CODE, branch);
		organizationRepository.save(agencyB);
		SettlementNode agencyBNode = SettlementNode.createChild(DEMO_AGENCY_B_NAME, agencyB,
				new BigDecimal("0.0300"), branchNode);
		settlementNodeRepository.save(agencyBNode);

		log.info("[DemoDataSeeder] Demo org tree created: HQ -> Branch -> [AgencyA, AgencyB]");
		return hq;
	}

	/**
	 * [NEW] Create 4 demo accounts. Roles per organization:
	 * - SUPER_ADMIN at HQ
	 * - ADMIN at HQ (operational HQ admin)
	 * - ADMIN at Branch
	 * - USER at AgencyA
	 *
	 * @param hq demo HQ (used to look up branch/agency by walking children)
	 * @author gayul.kim
	 */
	private void createDemoAccounts(Organization hq) {
		String encoded = passwordEncoder.encode(demoProperties.getPassword());

		Organization branch = organizationRepository.findByOrgCode(DEMO_BRANCH_CODE)
				.orElseThrow(() -> new IllegalStateException("Demo branch not found after seeding"));
		Organization agencyA = organizationRepository.findByOrgCode(DEMO_AGENCY_A_CODE)
				.orElseThrow(() -> new IllegalStateException("Demo agency A not found after seeding"));

		// SUPER_ADMIN at HQ (createSuperAdmin already sets ROLE_SUPER_ADMIN)
		User superUser = User.createSuperAdmin(DEMO_SUPER_EMAIL, encoded,
				"데모 슈퍼관리자", "010-0000-0001", hq);
		userRepository.save(superUser);

		// ADMIN at HQ
		User hqAdmin = User.createSuperAdmin(DEMO_HQ_EMAIL, encoded,
				"데모 본사관리자", "010-0000-0002", hq);
		hqAdmin.changeRole(UserRole.ROLE_ADMIN);
		userRepository.save(hqAdmin);

		// ADMIN at Branch
		User branchAdmin = User.createSuperAdmin(DEMO_BRANCH_EMAIL, encoded,
				"데모 지사관리자", "010-0000-0003", branch);
		branchAdmin.changeRole(UserRole.ROLE_ADMIN);
		userRepository.save(branchAdmin);

		// Regular USER at Agency A
		User agencyUser = User.createSuperAdmin(DEMO_AGENCY_EMAIL, encoded,
				"데모 대리점사용자", "010-0000-0004", agencyA);
		agencyUser.changeRole(UserRole.ROLE_USER);
		userRepository.save(agencyUser);

		log.info("[DemoDataSeeder] 4 demo accounts created: super/hq/branch/agency");
	}
}
