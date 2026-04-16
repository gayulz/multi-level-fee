package com.example.settlement.controller;

import com.example.settlement.domain.entity.SettlementNode;
import com.example.settlement.dto.NodeCreateRequest;
import com.example.settlement.service.SettlementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * [NEW] 정산 노드 관련 API 컨트롤러.
 *
 * 프론트엔드 비동기 폼에서 호출하는 노드 데이터를 처리합니다.
 *
 * @author gayul.kim
 * @since 2026-04-02
 */
@Slf4j
@RestController
@RequestMapping("/api/nodes")
@RequiredArgsConstructor
public class NodeApiController {

    private final SettlementService settlementService;

    /**
     * [NEW] 신규 정산 조직 등록.
     *
     * @param request 노드 생성 DTO
     * @return 생성 완료 메시지 및 ID
     */
    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> createNode(@Valid @RequestBody NodeCreateRequest request) {
        log.info("조직 생성 요청: {}", request);
        try {
            SettlementNode node = settlementService.createNode(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                            "message", "조직이 성공적으로 생성되었습니다.",
                            "nodeId", node.getId()
                    ));
        } catch (IllegalArgumentException e) {
            log.warn("조직 생성 검증 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("조직 생성 중 시스템 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "서버 오류가 발생했습니다. 로그를 확인해주세요."));
        }
    }

    /**
     * [NEW] 기존 정산 조직 수정.
     *
     * @param id      수정할 조직 ID
     * @param request 수정 요청 DTO
     * @return 수정 완료 메시지
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> updateNode(@PathVariable Long id, @Valid @RequestBody NodeCreateRequest request) {
        log.info("조직 수정 요청 - ID: {}, Data: {}", id, request);
        try {
            settlementService.updateNode(id, request);
            return ResponseEntity.ok(Map.of("message", "조직 정보가 성공적으로 수정되었습니다."));
        } catch (IllegalArgumentException e) {
            log.warn("조직 수정 검증 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("조직 수정 중 시스템 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "서버 오류가 발생했습니다."));
        }
    }
}
