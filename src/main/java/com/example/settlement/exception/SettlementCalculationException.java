package com.example.settlement.exception;

/**
 * [NEW] 정산 계산 중 논리적 오류가 발생했을 때 명확하게 구분하기 위한 예외.
 *
 * @author gayul.kim
 * @since 2026-03-06
 */
public class SettlementCalculationException extends RuntimeException {
    public SettlementCalculationException(String message) {
        super(message);
    }
}
