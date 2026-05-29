package com.example.settlement.demo;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static com.example.settlement.demo.DemoConstants.DEMO_ORDER_PREFIX;

/**
 * [NEW] 데모 정산 데이터 자동 리셋 스케줄러.
 *
 * <p>
 * 면접관과 리뷰어가 체험 도중 데모 데이터를 변경(승인/반려/생성)해 다른 방문자의
 * 체험을 망치는 상황을 방지하기 위해, 일정 주기마다 데모 정산 요청을 모두
 * 삭제한 뒤 시드를 재생성합니다. 조직과 사용자 자체는 유지합니다.
 * </p>
 *
 * <p>
 * 안전 가드:
 * - app.demo.enabled=true 일 때만 빈 등록 (@ConditionalOnProperty)
 * - 삭제 대상은 orderId LIKE 'DEMO-ORD-%' 인 행으로만 제한 → 일반 정산 절대 영향 X
 * - 스케줄 주기는 app.demo.reset-interval-minutes 로 외부 주입 가능
 * </p>
 *
 * <p>
 * 트레이드오프: 스케줄 주기를 짧게 하면 신선도가 좋지만 DB I/O가 늘어납니다.
 * 기본 30분은 면접 1회 세션 동안 일관성 유지 + 다음 세션 신선도 확보의 균형값입니다.
 * </p>
 *
 * @author gayul.kim
 * @since 2026-05-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.demo", name = "enabled", havingValue = "true")
public class DemoResetScheduler {

	private final EntityManager entityManager;
	private final DemoSettlementSeeder demoSettlementSeeder;

	/**
	 * [NEW] 데모 정산 요청을 주기적으로 리셋한다.
	 *
	 * <p>
	 * fixedDelayString 으로 SpEL 을 통해 분 단위 설정값을 ms 로 환산해 주입한다.
	 * initialDelayString 으로 앱 기동 직후 1주기를 건너뛰어 시드 직후 즉시 리셋되는
	 * 부작용을 막는다.
	 * </p>
	 *
	 * @author gayul.kim
	 */
	@Scheduled(
			fixedDelayString = "#{${app.demo.reset-interval-minutes:30} * 60 * 1000}",
			initialDelayString = "#{${app.demo.reset-interval-minutes:30} * 60 * 1000}")
	@Transactional
	public void resetDemoSettlements() {
		log.info("[DemoResetScheduler] === scheduled demo settlement reset start ===");

		int deleted = entityManager.createQuery(
						"DELETE FROM SettlementRequest s WHERE s.orderId LIKE :prefix")
				.setParameter("prefix", DEMO_ORDER_PREFIX + "%")
				.executeUpdate();
		entityManager.flush();
		entityManager.clear();

		log.info("[DemoResetScheduler] deleted {} demo settlement rows", deleted);

		demoSettlementSeeder.seedSettlements();

		log.info("[DemoResetScheduler] === scheduled demo settlement reset finished ===");
	}
}
