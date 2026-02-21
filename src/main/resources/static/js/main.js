/**
 * [NEW] Settlement Engine - 공통 JavaScript 유틸리티
 *
 * 모든 페이지에서 재사용 가능한 유틸리티 함수를 제공합니다.
 *
 * @author gayul.kim
 * @since 2026-02-21
 */

/**
 * Utils 네임스페이스: 공통 유틸리티 함수 모음
 */
const Utils = {
	/**
	 * 금액을 천 단위 구분 기호로 포맷팅합니다.
	 *
	 * @param {number} amount - 포맷팅할 금액
	 * @returns {string} 천 단위 구분 기호가 적용된 문자열 (예: "1,234,567")
	 */
	formatCurrency: function(amount) {
		if (amount === null || amount === undefined) {
			return '0';
		}
		return amount.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',');
	},

	/**
	 * 날짜/시간 문자열을 한국 형식으로 포맷팅합니다.
	 *
	 * @param {string|Date} dateString - 포맷팅할 날짜/시간
	 * @returns {string} 포맷팅된 날짜 문자열 (예: "2026-02-21 15:30:45")
	 */
	formatDate: function(dateString) {
		if (!dateString) {
			return '-';
		}

		const date = new Date(dateString);
		if (isNaN(date.getTime())) {
			return dateString; // 파싱 실패 시 원본 반환
		}

		const year = date.getFullYear();
		const month = String(date.getMonth() + 1).padStart(2, '0');
		const day = String(date.getDate()).padStart(2, '0');
		const hours = String(date.getHours()).padStart(2, '0');
		const minutes = String(date.getMinutes()).padStart(2, '0');
		const seconds = String(date.getSeconds()).padStart(2, '0');

		return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
	},

	/**
	 * Toast 알림을 표시합니다.
	 *
	 * @param {string} type - 알림 타입 ('success', 'error', 'warning', 'info')
	 * @param {string} message - 표시할 메시지
	 * @param {number} duration - 표시 시간 (밀리초, 기본값: 3000)
	 */
	showAlert: function(type, message, duration = 3000) {
		// 기존 알림 제거
		const existingAlert = document.getElementById('custom-toast-alert');
		if (existingAlert) {
			existingAlert.remove();
		}

		// 알림 타입별 Bootstrap 클래스 매핑
		const typeClassMap = {
			'success': 'alert-success',
			'error': 'alert-danger',
			'warning': 'alert-warning',
			'info': 'alert-info'
		};

		// 알림 타입별 아이콘 매핑
		const iconMap = {
			'success': 'fa-check-circle',
			'error': 'fa-exclamation-circle',
			'warning': 'fa-exclamation-triangle',
			'info': 'fa-info-circle'
		};

		const alertClass = typeClassMap[type] || 'alert-info';
		const icon = iconMap[type] || 'fa-info-circle';

		// 알림 요소 생성
		const alertElement = document.createElement('div');
		alertElement.id = 'custom-toast-alert';
		alertElement.className = `alert ${alertClass} alert-dismissible fade show position-fixed top-0 start-50 translate-middle-x mt-3`;
		alertElement.style.zIndex = '9999';
		alertElement.style.minWidth = '300px';
		alertElement.innerHTML = `
			<i class="fas ${icon} me-2"></i>
			<strong>${message}</strong>
			<button type="button" class="btn-close" data-bs-dismiss="alert"></button>
		`;

		// Body에 추가
		document.body.appendChild(alertElement);

		// 자동 제거
		setTimeout(() => {
			if (alertElement && alertElement.parentNode) {
				alertElement.classList.remove('show');
				setTimeout(() => alertElement.remove(), 150);
			}
		}, duration);
	},

	/**
	 * 전체 화면 로딩 오버레이를 표시합니다.
	 */
	showLoading: function() {
		let overlay = document.getElementById('global-loading-overlay');

		if (!overlay) {
			overlay = document.createElement('div');
			overlay.id = 'global-loading-overlay';
			overlay.className = 'loading-overlay';
			overlay.innerHTML = '<div class="loading-spinner"></div>';
			document.body.appendChild(overlay);
		}

		overlay.classList.add('show');
	},

	/**
	 * 전체 화면 로딩 오버레이를 숨깁니다.
	 */
	hideLoading: function() {
		const overlay = document.getElementById('global-loading-overlay');
		if (overlay) {
			overlay.classList.remove('show');
		}
	},

	/**
	 * URL 쿼리 파라미터를 객체로 파싱합니다.
	 *
	 * @returns {Object} 쿼리 파라미터 객체
	 */
	getQueryParams: function() {
		const params = {};
		const queryString = window.location.search.substring(1);
		const queries = queryString.split('&');

		queries.forEach(query => {
			const pair = query.split('=');
			if (pair[0]) {
				params[decodeURIComponent(pair[0])] = decodeURIComponent(pair[1] || '');
			}
		});

		return params;
	},

	/**
	 * 숫자를 소수점 자리수로 포맷팅합니다.
	 *
	 * @param {number} value - 포맷팅할 숫자
	 * @param {number} decimals - 소수점 자리수 (기본값: 2)
	 * @returns {string} 포맷팅된 숫자 문자열
	 */
	formatDecimal: function(value, decimals = 2) {
		if (value === null || value === undefined || isNaN(value)) {
			return '0.00';
		}
		return parseFloat(value).toFixed(decimals);
	}
};

/**
 * 페이지 로드 완료 시 초기화
 */
document.addEventListener('DOMContentLoaded', function() {
	console.log('Settlement Engine - Main.js loaded');

	// 모든 금액 요소에 포맷팅 적용 (data-money 속성 사용)
	document.querySelectorAll('[data-money]').forEach(element => {
		const amount = element.getAttribute('data-money');
		element.textContent = Utils.formatCurrency(amount);
	});

	// 모든 날짜 요소에 포맷팅 적용 (data-date 속성 사용)
	document.querySelectorAll('[data-date]').forEach(element => {
		const dateString = element.getAttribute('data-date');
		element.textContent = Utils.formatDate(dateString);
	});
});
