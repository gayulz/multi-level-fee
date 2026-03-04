/**
 * [NEW] 정산 시스템 커스텀 스크립트
 * 
 * 다크/라이트 테마 토글 및 로컬스토리지 상태 관리 지원
 * FOUC 방지를 위해 head에서 일부가 로드되거나, DOMContentLoaded시 조기 적용됩니다.
 *
 * @author gayul.kim
 * @since 2026-02-25
 */

const ThemeManager = {
	THEME_KEY: 'admin-dashboard-theme',

	init() {
		// 로컬스토리지에서 테마를 가져오거나 시스템 선호 테마를 사용
		const storedTheme = localStorage.getItem(this.THEME_KEY);
		const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
		const defaultTheme = storedTheme ? storedTheme : (prefersDark ? 'dark' : 'light');

		this.setTheme(defaultTheme);

		// DOM이 로드된 후 토글 버튼 이벤트 등록
		document.addEventListener('DOMContentLoaded', () => {
			this.updateToggleButton(defaultTheme);
			const toggleBtn = document.getElementById('theme-toggle');
			if (toggleBtn) {
				toggleBtn.addEventListener('click', () => this.toggleTheme());
			}
		});
	},

	setTheme(themeName) {
		document.documentElement.setAttribute('data-bs-theme', themeName);
		localStorage.setItem(this.THEME_KEY, themeName);
		this.updateToggleButton(themeName);
	},

	toggleTheme() {
		const currentTheme = document.documentElement.getAttribute('data-bs-theme');
		const newTheme = currentTheme === 'dark' ? 'light' : 'dark';
		this.setTheme(newTheme);
	},

	updateToggleButton(themeName) {
		const toggleIcon = document.getElementById('theme-toggle-icon');
		if (toggleIcon) {
			if (themeName === 'dark') {
				// 다크모드일 때는 '해' 아이콘을 보여주어 라이트모드로 전환 가능함을 표시
				toggleIcon.className = 'bi bi-sun-fill';
			} else {
				// 라이트모드일 때는 '달' 아이콘을 보여주어 다크모드로 전환 가능함을 표시
				toggleIcon.className = 'bi bi-moon-stars-fill';
			}
		}
	}
};

// 테마 스크립트를 즉시 로드 (FOUC 방지)
ThemeManager.init();

/**
 * [NEW] 공통 유틸리티 함수 모음
 *
 * 금액 포맷팅, 날짜 포맷팅, 동적 알림 표시 등
 * 프로젝트 내 모든 페이지에서 공통으로 사용합니다.
 *
 * @author gayul.kim
 * @since 2026-03-04
 */
const Utils = {
	/**
	 * 금액 포맷팅 (1000 → ₩1,000)
	 *
	 * @param {number} amount 포맷할 금액
	 * @return {string} 포맷된 금액 문자열
	 */
	formatCurrency(amount) {
		if (amount == null || isNaN(amount)) return '₩0';
		return '₩' + Number(amount).toLocaleString('ko-KR');
	},

	/**
	 * 날짜 포맷팅 (ISO 문자열 → yyyy-MM-dd HH:mm)
	 *
	 * @param {string} dateStr ISO 형식 날짜 문자열
	 * @return {string} 포맷된 날짜 문자열
	 */
	formatDate(dateStr) {
		if (!dateStr) return '-';
		const date = new Date(dateStr);
		const yyyy = date.getFullYear();
		const MM = String(date.getMonth() + 1).padStart(2, '0');
		const dd = String(date.getDate()).padStart(2, '0');
		const HH = String(date.getHours()).padStart(2, '0');
		const mm = String(date.getMinutes()).padStart(2, '0');
		return `${yyyy}-${MM}-${dd} ${HH}:${mm}`;
	},

	/**
	 * 동적 알림 표시
	 * Bootstrap 5 Alert를 페이지 상단에 동적으로 삽입합니다.
	 *
	 * @param {string} type 알림 유형 (success, warning, danger, info)
	 * @param {string} message 알림 메시지
	 * @param {number} autoDismissMs 자동 닫힘 시간 (ms, 0이면 수동 닫기)
	 */
	showAlert(type, message, autoDismissMs = 5000) {
		const iconMap = {
			success: 'bi-check-circle-fill',
			warning: 'bi-exclamation-triangle-fill',
			danger: 'bi-x-circle-fill',
			info: 'bi-info-circle-fill'
		};

		const alertDiv = document.createElement('div');
		alertDiv.className = `alert alert-${type} alert-dismissible fade show d-flex align-items-center gap-2 position-fixed top-0 start-50 translate-middle-x mt-3 shadow-lg`;
		alertDiv.style.zIndex = '9999';
		alertDiv.style.minWidth = '300px';
		alertDiv.style.maxWidth = '500px';
		alertDiv.setAttribute('role', 'alert');

		alertDiv.innerHTML = `
			<i class="bi ${iconMap[type] || iconMap.info} fs-5"></i>
			<div>${message}</div>
			<button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="닫기"></button>
		`;

		document.body.appendChild(alertDiv);

		if (autoDismissMs > 0) {
			setTimeout(() => {
				alertDiv.classList.remove('show');
				setTimeout(() => alertDiv.remove(), 300);
			}, autoDismissMs);
		}
	}
};

