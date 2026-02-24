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
