package com.example.settlement.exception;

/**
 * [NEW] 정산 노드를 찾을 수 없을 때 발생하는 예외.
 *
 * @author gayul.kim
 * @since 2026-03-06
 */
public class NodeNotFoundException extends RuntimeException {
    public NodeNotFoundException(String message) {
        super(message);
    }
}
