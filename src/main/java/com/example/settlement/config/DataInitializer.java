package com.example.settlement.config;

import com.example.settlement.domain.entity.Organization;
import com.example.settlement.domain.entity.SettlementNode;
import com.example.settlement.domain.entity.SettlementRequest;
import com.example.settlement.domain.entity.User;
import com.example.settlement.domain.entity.enums.UserRole;
import com.example.settlement.domain.repository.OrganizationRepository;
import com.example.settlement.domain.repository.SettlementNodeRepository;
import com.example.settlement.domain.repository.SettlementRequestRepository;
import com.example.settlement.domain.repository.UserRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * [MIG] 초기 데이터 생성기.
 *
 * <p>
 * 배포/테스트 환경 모두 동일한 DB를 사용하므로,
 * DB에 데이터가 없을 때 최초 1회만 실행되어 대규모 초기 데이터를 생성합니다.
 * </p>
 *
 * <p>
 * 생성 데이터:
 * - 조직: 본사 1 + 지점 3 + 대리점 (각 지점별 4~7개 랜덤)
 * - 사용자: 슈퍼관리자 1 + 본사관리자 5 + 지점관리자 9 + 대리점관리자 + 일반사용자
 * - 정산내역: 대리점발 500~700건 + 지사발 300~500건
 * </p>
 *
 * @author gayul.kim
 * @since 2026-04-02
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

	private final OrganizationRepository organizationRepository;
	private final SettlementNodeRepository settlementNodeRepository;
	private final SettlementRequestRepository settlementRequestRepository;
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final EntityManager entityManager;

	private final Random random = new Random();

	// =========================================================
	// 관리자/사용자 ID 인덱싱 카운터
	// =========================================================
	private int adminIndex = 1;
	private int userIndex = 1;

	// =========================================================
	// N+1 방지를 위한 메모리 리스트
	// =========================================================
	private final List<User> hqAdmins = new ArrayList<>();
	private final List<User> allBranchAdmins = new ArrayList<>();
	private final List<User> allAgencyAdmins = new ArrayList<>();
	private final List<User> allBranchUsers = new ArrayList<>();
	private final List<User> allAgencyUsers = new ArrayList<>();

	// 지점-대리점 매핑 (정산내역 생성 시 사용)
	private final Map<Organization, List<Organization>> branchAgencyMap = new LinkedHashMap<>();
	private final Map<Organization, SettlementNode> orgNodeMap = new LinkedHashMap<>();
	private final Map<Organization, List<User>> orgAdminMap = new LinkedHashMap<>();
	private final Map<Organization, List<User>> orgUserMap = new LinkedHashMap<>();

	// =========================================================
	// 한국 이름 데이터
	// =========================================================
	private static final String[] LAST_NAMES = {
		"유", "고", "문", "양", "손", "김", "배", "조", "백", "허",
		"남", "심", "곽", "노", "정", "하", "성", "차", "주", "우",
		"최", "이", "윤", "장", "임", "한", "신", "오", "서"
	};

	private static final String[] FIRST_NAME_PARTS_1 = {
		"서", "민", "지", "현", "수", "영", "준", "하", "은", "도",
		"태", "유", "성", "재", "승", "정", "예", "시", "혜", "진",
		"건", "우", "상", "채", "인", "소", "다", "나", "미", "원"
	};

	private static final String[] FIRST_NAME_PARTS_2 = {
		"연", "호", "진", "아", "빈", "원", "준", "우", "율", "서",
		"영", "희", "은", "경", "환", "혁", "린", "솔", "담", "결",
		"수", "민", "지", "현", "석", "택", "용", "기", "한", "윤"
	};

	// =========================================================
	// 지점별 대리점 후보 지역명
	// =========================================================
	private static final String[] SEOUL_AGENCIES = {"서울", "인천", "수원", "용인", "분당", "광명", "파주"};
	private static final String[] GYEONGSANG_AGENCIES = {"포항", "울산", "부산", "구미", "대구", "창원", "경주"};
	private static final String[] JEOLLA_AGENCIES = {"광주", "목포", "전주", "순천", "익산", "군산", "김제"};

	// =========================================================
	// 전화번호 생성용
	// =========================================================
	private static final String[] PHONE_MIDDLE = {"1234", "5678", "9012", "3456", "7890", "2345", "6789", "0123", "4567", "8901"};

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		// ====================================================
		// 최초 실행 여부 판단: DB에 Organization이 1건이라도 있으면 SKIP
		// ====================================================
		if (organizationRepository.count() > 0) {
			log.info("========================================");
			log.info("[DataInitializer] 기존 데이터가 존재합니다. 초기화를 건너뜁니다.");
			log.info("========================================");
			return;
		}

		log.info("========================================");
		log.info("[DataInitializer] 최초 실행 - 초기 데이터 생성 시작");
		log.info("========================================");

		long startTime = System.currentTimeMillis();

		// 비밀번호 인코딩 (2종류)
		String superAdminPw = passwordEncoder.encode("Qhdqhddlsp**");
		String commonPw = passwordEncoder.encode("Qhdqhddlsp1234**");
		log.info("[DataInitializer] 비밀번호 인코딩 완료");

		// ====================================================
		// 1. 조직 + 노드 + 사용자 생성
		// ====================================================
		createOrganizationsAndUsers(superAdminPw, commonPw);

		// ====================================================
		// 2. 정산 내역 생성
		// ====================================================
		createSettlementRequests(commonPw);

		long elapsed = System.currentTimeMillis() - startTime;
		log.info("========================================");
		log.info("[DataInitializer] 초기 데이터 생성 완료 (소요시간: {}ms)", elapsed);
		log.info("========================================");
	}

	// =========================================================
	// 1. 조직 + 노드 + 사용자 생성
	// =========================================================
	private void createOrganizationsAndUsers(String superAdminPw, String commonPw) {
		// ─── (1) 본사 ───
		Organization hq = Organization.createHeadquarters("SattleTree 본사", "HQ-001");
		organizationRepository.save(hq);

		SettlementNode hqNode = SettlementNode.createRoot("본사 노드", hq, new BigDecimal("0.1000"));
		settlementNodeRepository.save(hqNode);
		orgNodeMap.put(hq, hqNode);

		// 슈퍼관리자 1명 (ID: admin)
		User superAdmin = User.createSuperAdmin("admin", superAdminPw, "운영자", "010-0000-0000", hq);
		userRepository.save(superAdmin);
		hqAdmins.add(superAdmin);

		// 본사 관리자 5명
		List<User> hqAdminList = createAdminUsers(5, hq, commonPw);
		hqAdmins.addAll(hqAdminList);
		orgAdminMap.put(hq, new ArrayList<>(hqAdmins));

		// 본사 사용자 20명
		List<User> hqUserList = createNormalUsers(20, hq, commonPw);
		orgUserMap.put(hq, hqUserList);

		log.info("[DataInitializer] 본사 생성 완료 - 관리자: {}명, 사용자: {}명",
			hqAdmins.size(), hqUserList.size());

		// ─── (2) 지점 3개 ───
		String[][] branchConfig = {
			{"서울경기 지점", "BR-001"},
			{"경상도 지점", "BR-002"},
			{"전라도 지점", "BR-003"}
		};
		String[][] agencySets = {SEOUL_AGENCIES, GYEONGSANG_AGENCIES, JEOLLA_AGENCIES};

		for (int b = 0; b < 3; b++) {
			Organization branch = Organization.createBranch(branchConfig[b][0], branchConfig[b][1], hq);
			organizationRepository.save(branch);

			SettlementNode branchNode = SettlementNode.createChild(
				branchConfig[b][0] + " 노드", branch, new BigDecimal("0.0500"), hqNode
			);
			settlementNodeRepository.save(branchNode);
			orgNodeMap.put(branch, branchNode);

			// 지점 관리자 3명
			List<User> branchAdmins = createAdminUsers(3, branch, commonPw);
			allBranchAdmins.addAll(branchAdmins);
			orgAdminMap.put(branch, branchAdmins);

			// 지점 사용자 20~30명 랜덤
			int branchUserCount = random.nextInt(11) + 20;
			List<User> branchUserList = createNormalUsers(branchUserCount, branch, commonPw);
			allBranchUsers.addAll(branchUserList);
			orgUserMap.put(branch, branchUserList);

			log.info("[DataInitializer] {} 생성 완료 - 관리자: {}명, 사용자: {}명",
				branchConfig[b][0], branchAdmins.size(), branchUserList.size());

			// ─── (3) 대리점 (지점별 4~7개 랜덤) ───
			int agencyCount = random.nextInt(4) + 4; // 4~7
			List<String> agencyCandidates = new ArrayList<>(Arrays.asList(agencySets[b]));
			Collections.shuffle(agencyCandidates, random);

			List<Organization> agencies = new ArrayList<>();
			for (int a = 0; a < agencyCount; a++) {
				String agencyName = agencyCandidates.get(a) + " 대리점";
				String agencyCode = "AG-" + (b + 1) + String.format("%02d", a + 1);

				Organization agency = Organization.createAgency(agencyName, agencyCode, branch);
				organizationRepository.save(agency);

				// 대리점 수수료: 1~3% 랜덤 (0.0100 ~ 0.0300)
				int feePercent = random.nextInt(3) + 1;
				BigDecimal agencyFeeRate = new BigDecimal("0.0" + feePercent + "00");
				SettlementNode agencyNode = SettlementNode.createChild(
					agencyName + " 노드", agency, agencyFeeRate, branchNode
				);
				settlementNodeRepository.save(agencyNode);
				orgNodeMap.put(agency, agencyNode);

				// 대리점 관리자 2명
				List<User> agencyAdmins = createAdminUsers(2, agency, commonPw);
				allAgencyAdmins.addAll(agencyAdmins);
				orgAdminMap.put(agency, agencyAdmins);

				// 대리점 사용자 30~50명 랜덤
				int agUserCount = random.nextInt(21) + 30;
				List<User> agencyUserList = createNormalUsers(agUserCount, agency, commonPw);
				allAgencyUsers.addAll(agencyUserList);
				orgUserMap.put(agency, agencyUserList);

				agencies.add(agency);

				log.info("[DataInitializer]   └ {} 생성 (수수료: {}%) - 관리자: {}명, 사용자: {}명",
					agencyName, feePercent, agencyAdmins.size(), agencyUserList.size());
			}

			branchAgencyMap.put(branch, agencies);
		}

		entityManager.flush();
		entityManager.clear();

		int totalUsers = hqAdmins.size() + orgUserMap.get(hq).size()
			+ allBranchAdmins.size() + allBranchUsers.size()
			+ allAgencyAdmins.size() + allAgencyUsers.size();

		log.info("[DataInitializer] ──── 전체 사용자 생성 완료: {}명 ────", totalUsers);
	}

	// =========================================================
	// 2. 정산 내역 생성
	// =========================================================
	private void createSettlementRequests(String commonPw) {
		log.info("[DataInitializer] 정산 내역 생성 시작...");

		// (1) 대리점발 (계층형: 본사-지사-대리점 모든 노드에 배분) 500~700건
		int agencyRequestCount = random.nextInt(201) + 500;
		generateAgencyBasedRequests(agencyRequestCount);

		// (2) 지사발 (지사형: 본사-지사 노드에만 배분) 300~500건
		int branchRequestCount = random.nextInt(201) + 300;
		generateBranchBasedRequests(branchRequestCount);

		entityManager.flush();
		entityManager.clear();

		log.info("[DataInitializer] 정산 내역 생성 완료 (대리점발: {}건, 지사발: {}건)",
			agencyRequestCount, branchRequestCount);
	}

	/**
	 * [NEW] 대리점발 정산 요청 생성 (계층형).
	 * 대리점 사용자가 요청 → 대리점관리자 → 지사관리자 → 본사관리자 순으로 승인 진행.
	 *
	 * @param count 생성할 건수
	 * @author gayul.kim
	 */
	private void generateAgencyBasedRequests(int count) {
		if (allAgencyUsers.isEmpty()) return;

		LocalDateTime now = LocalDateTime.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

		log.info("[DataInitializer]   대리점발 정산 생성 시작 (목표: {}건)", count);

		for (int i = 0; i < count; i++) {
			// 랜덤 대리점 사용자 선택
			User requester = allAgencyUsers.get(random.nextInt(allAgencyUsers.size()));
			Organization agency = requester.getOrganization();
			SettlementNode rootNode = orgNodeMap.get(agency);

			// 루트 노드를 못 찾으면 상위의 본사 노드 사용
			if (rootNode == null) {
				rootNode = orgNodeMap.values().iterator().next();
			}

			BigDecimal amount = BigDecimal.valueOf(random.nextInt(900001) + 100000);
			String orderId = "ORD-AG-" + now.format(formatter) + "-" + String.format("%06d", i + 1);
			LocalDateTime createdDate = now.minusDays(random.nextInt(30)).minusHours(random.nextInt(24));

			SettlementRequest request = SettlementRequest.create(
				orderId, amount, "대리점발 정산 #" + (i + 1), requester, agency, rootNode
			);

			// 생성일자 분산 (리플렉션)
			setCreatedAt(request, createdDate);

			// 상태 배분: 80% 승인, 20% 나머지 (대기/반려 랜덤)
			applyAgencyApprovalFlow(request, agency);

			settlementRequestRepository.save(request);

			if ((i + 1) % 100 == 0) {
				log.info("[DataInitializer]   대리점발 생성 중... ({}/{})", i + 1, count);
			}
		}
		log.info("[DataInitializer]   대리점발 정산 생성 완료");
	}

	/**
	 * [NEW] 지사발 정산 요청 생성 (지사형).
	 * 지사 사용자가 요청 → 지사관리자 → 본사관리자 순으로 승인 진행.
	 *
	 * @param count 생성할 건수
	 * @author gayul.kim
	 */
	private void generateBranchBasedRequests(int count) {
		if (allBranchUsers.isEmpty()) return;

		LocalDateTime now = LocalDateTime.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

		log.info("[DataInitializer]   지사발 정산 생성 시작 (목표: {}건)", count);

		for (int i = 0; i < count; i++) {
			// 랜덤 지사 사용자 선택
			User requester = allBranchUsers.get(random.nextInt(allBranchUsers.size()));
			Organization branch = requester.getOrganization();
			SettlementNode rootNode = orgNodeMap.get(branch);

			if (rootNode == null) {
				rootNode = orgNodeMap.values().iterator().next();
			}

			BigDecimal amount = BigDecimal.valueOf(random.nextInt(900001) + 100000);
			String orderId = "ORD-BR-" + now.format(formatter) + "-" + String.format("%06d", i + 1);
			LocalDateTime createdDate = now.minusDays(random.nextInt(30)).minusHours(random.nextInt(24));

			SettlementRequest request = SettlementRequest.create(
				orderId, amount, "지사발 정산 #" + (i + 1), requester, branch, rootNode
			);

			// 생성일자 분산
			setCreatedAt(request, createdDate);

			// 상태 배분: 80% 승인, 20% 나머지
			applyBranchApprovalFlow(request, branch);

			settlementRequestRepository.save(request);

			if ((i + 1) % 100 == 0) {
				log.info("[DataInitializer]   지사발 생성 중... ({}/{})", i + 1, count);
			}
		}
		log.info("[DataInitializer]   지사발 정산 생성 완료");
	}

	// =========================================================
	// 승인 프로세스 흐름 적용
	// =========================================================

	/**
	 * [NEW] 대리점발 정산의 3단계 승인 흐름을 적용합니다.
	 * PENDING → AGENCY_APPROVED → BRANCH_APPROVED → COMPLETED
	 * 80%는 COMPLETED까지, 나머지 20%는 PENDING/REJECTED 랜덤.
	 *
	 * @param request 정산 요청
	 * @param agency  요청 대리점
	 * @author gayul.kim
	 */
	private void applyAgencyApprovalFlow(SettlementRequest request, Organization agency) {
		boolean isApproved = random.nextInt(100) < 80;

		if (isApproved) {
			// ── 3단계 승인 완료 ──
			// Step 1: 대리점 관리자 승인
			User agencyAdmin = findRandomAdmin(agency);
			if (agencyAdmin != null) {
				request.approve(agencyAdmin, "대리점 승인 완료");
			}

			// Step 2: 지사 관리자 승인
			Organization branch = agency.getParent();
			User branchAdmin = findRandomAdmin(branch);
			if (branchAdmin != null) {
				request.approve(branchAdmin, "지사 승인 완료");
			}

			// Step 3: 본사 관리자 승인
			User hqAdmin = hqAdmins.get(random.nextInt(hqAdmins.size()));
			request.approve(hqAdmin, "본사 최종 승인");

			// 수수료 설정
			BigDecimal feeRate = new BigDecimal("0.05");
			request.setSettlementAmounts(
				request.getAmount().multiply(feeRate),
				request.getAmount().subtract(request.getAmount().multiply(feeRate))
			);
		} else {
			// ── 미완료 건 ── 대기/반려 랜덤
			int subCase = random.nextInt(3);
			switch (subCase) {
				case 0:
					// PENDING 상태 유지 (대기)
					break;
				case 1:
					// 대리점 승인 후 지사에서 반려
					User agAdmin = findRandomAdmin(agency);
					if (agAdmin != null) {
						request.approve(agAdmin, "대리점 승인");
					}
					Organization br = agency.getParent();
					User brAdmin = findRandomAdmin(br);
					if (brAdmin != null) {
						request.reject(brAdmin, "요건 미달로 반려");
					}
					break;
				case 2:
					// 대리점 승인 → 지사 승인까지만 (정체)
					User ag2 = findRandomAdmin(agency);
					if (ag2 != null) {
						request.approve(ag2, "대리점 승인");
					}
					Organization br2 = agency.getParent();
					User br2Admin = findRandomAdmin(br2);
					if (br2Admin != null) {
						request.approve(br2Admin, "지사 승인");
					}
					// 본사 승인 대기 상태로 정체
					break;
			}
		}
	}

	/**
	 * [NEW] 지사발 정산의 2단계 승인 흐름을 적용합니다.
	 * PENDING → BRANCH_APPROVED → COMPLETED
	 * 80%는 COMPLETED까지, 나머지 20%는 PENDING/REJECTED 랜덤.
	 *
	 * @param request 정산 요청
	 * @param branch  요청 지사
	 * @author gayul.kim
	 */
	private void applyBranchApprovalFlow(SettlementRequest request, Organization branch) {
		boolean isApproved = random.nextInt(100) < 80;

		if (isApproved) {
			// ── 2단계 승인 완료 ──
			// Step 1: 지사 관리자 승인
			User branchAdmin = findRandomAdmin(branch);
			if (branchAdmin != null) {
				request.approve(branchAdmin, "지사 승인 완료");
			}

			// Step 2: 본사 관리자 승인
			User hqAdmin = hqAdmins.get(random.nextInt(hqAdmins.size()));
			request.approve(hqAdmin, "본사 최종 승인");

			// 수수료 설정
			BigDecimal feeRate = new BigDecimal("0.05");
			request.setSettlementAmounts(
				request.getAmount().multiply(feeRate),
				request.getAmount().subtract(request.getAmount().multiply(feeRate))
			);
		} else {
			int subCase = random.nextInt(3);
			switch (subCase) {
				case 0:
					// PENDING 상태 유지 (대기)
					break;
				case 1:
					// 지사에서 반려
					User brAdmin = findRandomAdmin(branch);
					if (brAdmin != null) {
						request.reject(brAdmin, "증빙 서류 미비로 반려");
					}
					break;
				case 2:
					// 지사 승인 후 본사 대기 (정체)
					User brAdmin2 = findRandomAdmin(branch);
					if (brAdmin2 != null) {
						request.approve(brAdmin2, "지사 승인");
					}
					// 본사 승인 대기 상태로 정체
					break;
			}
		}
	}

	// =========================================================
	// 유틸리티 메서드
	// =========================================================

	/**
	 * [NEW] 관리자 사용자 생성.
	 * ID 패턴: admin{인덱스}@sattletree.io
	 *
	 * @param count 생성 수
	 * @param org   소속 조직
	 * @param password 인코딩된 비밀번호
	 * @return 생성된 관리자 목록
	 * @author gayul.kim
	 */
	private List<User> createAdminUsers(int count, Organization org, String password) {
		List<User> admins = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			String email = "admin" + adminIndex + "@sattletree.io";
			String koreanName = generateKoreanName();
			String phone = generatePhone();

			User admin = User.createSuperAdmin(email, password, koreanName, phone, org);
			admin.changeRole(UserRole.ROLE_ADMIN);
			userRepository.save(admin);
			admins.add(admin);
			adminIndex++;
		}
		return admins;
	}

	/**
	 * [NEW] 일반 사용자 생성.
	 * ID 패턴: user{인덱스}@sattletree.io
	 *
	 * @param count 생성 수
	 * @param org   소속 조직
	 * @param password 인코딩된 비밀번호
	 * @return 생성된 사용자 목록
	 * @author gayul.kim
	 */
	private List<User> createNormalUsers(int count, Organization org, String password) {
		List<User> users = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			String email = "user" + userIndex + "@sattletree.io";
			String koreanName = generateKoreanName();
			String phone = generatePhone();

			User user = User.createSuperAdmin(email, password, koreanName, phone, org);
			user.changeRole(UserRole.ROLE_USER);
			userRepository.save(user);
			users.add(user);
			userIndex++;
		}
		return users;
	}

	/**
	 * [NEW] 한국 이름 생성 (성씨 + 이름 2글자).
	 *
	 * @return 생성된 한국 이름 (예: "김서연")
	 * @author gayul.kim
	 */
	private String generateKoreanName() {
		String lastName = LAST_NAMES[random.nextInt(LAST_NAMES.length)];
		String firstName1 = FIRST_NAME_PARTS_1[random.nextInt(FIRST_NAME_PARTS_1.length)];
		String firstName2 = FIRST_NAME_PARTS_2[random.nextInt(FIRST_NAME_PARTS_2.length)];
		return lastName + firstName1 + firstName2;
	}

	/**
	 * [NEW] 전화번호 생성.
	 *
	 * @return 010-XXXX-XXXX 형식의 전화번호
	 * @author gayul.kim
	 */
	private String generatePhone() {
		return "010-" + PHONE_MIDDLE[random.nextInt(PHONE_MIDDLE.length)]
			+ "-" + String.format("%04d", random.nextInt(10000));
	}

	/**
	 * [NEW] 조직에서 랜덤 관리자 1명 반환.
	 *
	 * @param org 조직
	 * @return 해당 조직의 관리자 (없으면 null)
	 * @author gayul.kim
	 */
	private User findRandomAdmin(Organization org) {
		if (org == null) return null;
		List<User> admins = orgAdminMap.get(org);
		if (admins == null || admins.isEmpty()) return null;
		return admins.get(random.nextInt(admins.size()));
	}

	/**
	 * [NEW] 리플렉션으로 createdAt 값을 설정 (날짜 분산용).
	 *
	 * @param request    정산 요청
	 * @param createdAt  설정할 생성일시
	 * @author gayul.kim
	 */
	private void setCreatedAt(SettlementRequest request, LocalDateTime createdAt) {
		try {
			java.lang.reflect.Field createdAtField = SettlementRequest.class.getDeclaredField("createdAt");
			createdAtField.setAccessible(true);
			createdAtField.set(request, createdAt);
		} catch (Exception e) {
			log.warn("[DataInitializer] createdAt 설정 실패: {}", e.getMessage());
		}
	}
}
