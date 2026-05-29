package com.example.settlement.demo;

/**
 * [NEW] 데모 모드 상수 정의.
 *
 * <p>
 * 데모 계정 이메일, 데모 조직 코드, 정산 시드 건수 등 데모 모드에서 사용되는
 * 상수를 한 곳에 모아 관리합니다. 운영 환경에서 실수로 데모 데이터가 일반
 * 사용자 데이터와 섞이지 않도록 식별용 prefix(DEMO-, demo_)를 유지합니다.
 * </p>
 *
 * @author gayul.kim
 * @since 2026-05-30
 */
public final class DemoConstants {

	private DemoConstants() {
	}

	// 데모 조직 코드 (운영 데이터와 식별 가능하도록 DEMO- prefix 부착)
	public static final String DEMO_HQ_CODE = "DEMO-HQ-001";
	public static final String DEMO_BRANCH_CODE = "DEMO-BR-001";
	public static final String DEMO_AGENCY_A_CODE = "DEMO-AG-001";
	public static final String DEMO_AGENCY_B_CODE = "DEMO-AG-002";

	public static final String DEMO_HQ_NAME = "데모 본사";
	public static final String DEMO_BRANCH_NAME = "데모 서울지사";
	public static final String DEMO_AGENCY_A_NAME = "데모 강남대리점";
	public static final String DEMO_AGENCY_B_NAME = "데모 분당대리점";

	// 데모 계정 이메일 (역할별 1개씩)
	public static final String DEMO_SUPER_EMAIL = "demo_super@settletree.io";
	public static final String DEMO_HQ_EMAIL = "demo_hq@settletree.io";
	public static final String DEMO_BRANCH_EMAIL = "demo_branch@settletree.io";
	public static final String DEMO_AGENCY_EMAIL = "demo_agency@settletree.io";

	// 데모 정산 요청 시드 건수
	public static final int DEMO_SETTLEMENT_COUNT = 50;

	// 데모 계정 식별 prefix - 데이터 리셋 시 안전 가드로 사용
	public static final String DEMO_EMAIL_PREFIX = "demo_";
	public static final String DEMO_ORG_CODE_PREFIX = "DEMO-";
	public static final String DEMO_ORDER_PREFIX = "DEMO-ORD-";
}
