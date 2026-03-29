/**
 * [NEW] 대시보드 JavaScript
 *
 * 대시보드 페이지 인터랙션 처리
 *
 * @author gayul.kim
 * @since 2025-01-30
 */

(function() {
	'use strict';

	// DOM 로드 완료 시 초기화
	document.addEventListener('DOMContentLoaded', function() {
		initDashboard();
	});

	/**
	 * 대시보드 초기화
	 */
	function initDashboard() {
		// 통계 카드 애니메이션
		animateStatCards();

		// 조직 트리 토글 기능 (있을 경우)
		initOrgTreeToggle();

		// 테이블 행 클릭 이벤트
		initTableRowClick();
	}

	/**
	 * 통계 카드 숫자 애니메이션
	 */
	function animateStatCards() {
		const statValues = document.querySelectorAll('.stat-value, .stat-value-lg');

		statValues.forEach(element => {
			const targetValue = parseInt(element.textContent.replace(/,/g, ''));

			if (isNaN(targetValue)) {
				return;
			}

			let currentValue = 0;
			const increment = targetValue / 50;  // 50단계로 애니메이션
			const duration = 1000;  // 1초
			const stepTime = duration / 50;

			const timer = setInterval(() => {
				currentValue += increment;

				if (currentValue >= targetValue) {
					element.textContent = formatNumber(targetValue);
					clearInterval(timer);
				} else {
					element.textContent = formatNumber(Math.floor(currentValue));
				}
			}, stepTime);
		});
	}

	/**
	 * 조직 트리 토글 기능
	 */
	function initOrgTreeToggle() {
		const orgNodes = document.querySelectorAll('.org-node');

		orgNodes.forEach(node => {
			node.addEventListener('click', function(e) {
				e.stopPropagation();

				const children = this.nextElementSibling;

				if (children && children.classList.contains('children')) {
					children.classList.toggle('hidden');

					// 토글 아이콘 변경 (있을 경우)
					const icon = this.querySelector('.toggle-icon');
					if (icon) {
						icon.classList.toggle('rotate-90');
					}
				}
			});
		});
	}

	/**
	 * 테이블 행 클릭 이벤트 (상세 페이지로 이동)
	 */
	function initTableRowClick() {
		const tableRows = document.querySelectorAll('.table tbody tr');

		tableRows.forEach(row => {
			row.style.cursor = 'pointer';

			row.addEventListener('click', function() {
				const settlementId = this.dataset.settlementId;

				if (settlementId) {
					window.location.href = `/settlements/${settlementId}`;
				}
			});
		});
	}

	/**
	 * 숫자 포맷팅 (천단위 쉼표)
	 */
	function formatNumber(num) {
		return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',');
	}

	/**
	 * 페이지 새로고침 (통계 업데이트용)
	 */
	window.refreshDashboard = function() {
		location.reload();
	};

	/**
	 * 특정 통계 카드 업데이트 (AJAX)
	 */
	window.updateStatCard = function(cardId, newValue) {
		const card = document.getElementById(cardId);

		if (card) {
			const valueElement = card.querySelector('.stat-value, .stat-value-lg');

			if (valueElement) {
				valueElement.textContent = formatNumber(newValue);
			}
		}
	};
})();
