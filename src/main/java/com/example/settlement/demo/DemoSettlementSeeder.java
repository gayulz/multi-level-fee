package com.example.settlement.demo;

import com.example.settlement.domain.entity.Organization;
import com.example.settlement.domain.entity.SettlementNode;
import com.example.settlement.domain.entity.SettlementRequest;
import com.example.settlement.domain.entity.User;
import com.example.settlement.domain.repository.OrganizationRepository;
import com.example.settlement.domain.repository.SettlementNodeRepository;
import com.example.settlement.domain.repository.SettlementRequestRepository;
import com.example.settlement.domain.repository.UserRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

import static com.example.settlement.demo.DemoConstants.*;

/**
 * [NEW] 데모 정산 요청 시더.
 *
 * <p>
 * 데모 조직 트리(AgencyA, Branch)에서 발생한 정산 요청을 시드합니다.
 * 상태 분포는 PENDING / 부분승인 / COMPLETED / REJECTED 가 골고루 섞이도록 합니다.
 * 시드 단위로 분리한 이유는 DemoDataSeeder 의 줄 수 한도(300줄) 여유와
 * PR2의 자동 리셋 스케줄러에서 재사용하기 위함입니다.
 * </p>
 *
 * @author gayul.kim
 * @since 2026-05-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DemoSettlementSeeder {

	private final OrganizationRepository organizationRepository;
	private final SettlementNodeRepository settlementNodeRepository;
	private final SettlementRequestRepository settlementRequestRepository;
	private final UserRepository userRepository;
	private final EntityManager entityManager;

	private final Random random = new Random(42L);

	/**
	 * [NEW] Seed demo settlement requests against the demo org tree.
	 * Idempotent at the org-code level: requires the demo org tree to already exist.
	 *
	 * @author gayul.kim
	 */
	@Transactional
	public void seedSettlements() {
		Organization agencyA = organizationRepository.findByOrgCode(DEMO_AGENCY_A_CODE).orElse(null);
		Organization branch = organizationRepository.findByOrgCode(DEMO_BRANCH_CODE).orElse(null);
		if (agencyA == null || branch == null) {
			log.warn("[DemoSettlementSeeder] demo org tree missing - skip settlement seeding");
			return;
		}

		List<User> agencyUsers = userRepository.findByOrganization(agencyA);
		List<User> branchUsers = userRepository.findByOrganization(branch);
		if (agencyUsers.isEmpty()) {
			log.warn("[DemoSettlementSeeder] no demo agency users - skip");
			return;
		}

		SettlementNode agencyNode = settlementNodeRepository
				.findAll().stream()
				.filter(n -> n.getOrganization() != null
						&& DEMO_AGENCY_A_CODE.equals(n.getOrganization().getOrgCode()))
				.findFirst().orElse(null);
		SettlementNode branchNode = settlementNodeRepository
				.findAll().stream()
				.filter(n -> n.getOrganization() != null
						&& DEMO_BRANCH_CODE.equals(n.getOrganization().getOrgCode()))
				.findFirst().orElse(null);

		if (agencyNode == null || branchNode == null) {
			log.warn("[DemoSettlementSeeder] demo settlement nodes missing - skip");
			return;
		}

		int created = 0;
		for (int i = 0; i < DEMO_SETTLEMENT_COUNT; i++) {
			boolean fromAgency = random.nextInt(100) < 70; // 70% agency-originated
			User requester = fromAgency
					? agencyUsers.get(random.nextInt(agencyUsers.size()))
					: (branchUsers.isEmpty()
							? agencyUsers.get(random.nextInt(agencyUsers.size()))
							: branchUsers.get(random.nextInt(branchUsers.size())));
			Organization org = requester.getOrganization();
			SettlementNode rootNode = fromAgency ? agencyNode : branchNode;

			BigDecimal amount = BigDecimal.valueOf(100000L + random.nextInt(900_000));
			String orderId = nextOrderId(i);
			SettlementRequest req = SettlementRequest.create(
					orderId, amount, "데모 정산 #" + (i + 1), requester, org, rootNode);

			// Distribute createdAt over the last 30 days for a more lively dashboard
			setCreatedAt(req, LocalDateTime.now().minusDays(random.nextInt(30))
					.minusHours(random.nextInt(24)));

			applyDemoApprovalFlow(req, fromAgency);
			settlementRequestRepository.save(req);
			created++;
		}

		entityManager.flush();
		log.info("[DemoSettlementSeeder] {} demo settlements created", created);
	}

	/**
	 * [NEW] Apply a representative approval flow so the demo dashboard shows
	 * a healthy mix of states (PENDING / partially approved / COMPLETED / REJECTED).
	 *
	 * @param req        settlement request
	 * @param fromAgency true if originated from agency (3-step flow), false for branch (2-step)
	 * @author gayul.kim
	 */
	private void applyDemoApprovalFlow(SettlementRequest req, boolean fromAgency) {
		int dice = random.nextInt(10);
		User hqAdmin = userRepository.findByEmail(DEMO_HQ_EMAIL).orElse(null);
		User branchAdmin = userRepository.findByEmail(DEMO_BRANCH_EMAIL).orElse(null);
		User agencyUser = userRepository.findByEmail(DEMO_AGENCY_EMAIL).orElse(null);

		if (fromAgency) {
			if (dice < 5 && hqAdmin != null && branchAdmin != null && agencyUser != null) {
				// 50% completed
				req.approve(agencyUser, "데모 대리점 승인");
				req.approve(branchAdmin, "데모 지사 승인");
				req.approve(hqAdmin, "데모 본사 최종 승인");
				BigDecimal feeRate = new BigDecimal("0.05");
				req.setSettlementAmounts(
						req.getAmount().multiply(feeRate),
						req.getAmount().subtract(req.getAmount().multiply(feeRate)));
			} else if (dice < 7 && agencyUser != null) {
				// 20% pending after agency approval
				req.approve(agencyUser, "데모 대리점 승인");
			} else if (dice < 9 && agencyUser != null && branchAdmin != null) {
				// 20% pending after branch approval (waiting HQ)
				req.approve(agencyUser, "데모 대리점 승인");
				req.approve(branchAdmin, "데모 지사 승인");
			} else if (branchAdmin != null && agencyUser != null) {
				// 10% rejected at branch
				req.approve(agencyUser, "데모 대리점 승인");
				req.reject(branchAdmin, "데모 반려 - 증빙 미비");
			}
		} else {
			if (dice < 6 && hqAdmin != null && branchAdmin != null) {
				req.approve(branchAdmin, "데모 지사 승인");
				req.approve(hqAdmin, "데모 본사 최종 승인");
				BigDecimal feeRate = new BigDecimal("0.05");
				req.setSettlementAmounts(
						req.getAmount().multiply(feeRate),
						req.getAmount().subtract(req.getAmount().multiply(feeRate)));
			} else if (dice < 9 && branchAdmin != null) {
				req.approve(branchAdmin, "데모 지사 승인");
			} else if (branchAdmin != null) {
				req.reject(branchAdmin, "데모 반려");
			}
		}
	}

	private String nextOrderId(int index) {
		DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
		return DEMO_ORDER_PREFIX + LocalDateTime.now().format(f) + "-" + String.format("%05d", index);
	}

	/**
	 * [NEW] Use reflection to spread createdAt across recent days so dashboards
	 * show movement over time. JPA auto-populates createdAt by default which
	 * would cluster every request at "now".
	 *
	 * @param req       settlement request to mutate
	 * @param createdAt the synthetic creation timestamp
	 * @author gayul.kim
	 */
	private void setCreatedAt(SettlementRequest req, LocalDateTime createdAt) {
		try {
			Field f = SettlementRequest.class.getDeclaredField("createdAt");
			f.setAccessible(true);
			f.set(req, createdAt);
		} catch (Exception e) {
			log.debug("[DemoSettlementSeeder] createdAt override failed: {}", e.getMessage());
		}
	}
}
