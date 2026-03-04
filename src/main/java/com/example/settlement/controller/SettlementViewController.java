package com.example.settlement.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;

/**
 * [NEW] 정산 요청/결과 뷰 컨트롤러
 *
 * 정산 요청 폼 페이지 렌더링 및 Mock 데이터를 제공합니다.
 * 백엔드 연동 전까지 Mock 데이터 기반으로 UI를 검증합니다.
 *
 * @author gayul.kim
 * @since 2026-03-04
 */
@Controller
@RequestMapping("/settlement")
public class SettlementViewController {

    /**
     * [NEW] 정산 요청 폼 페이지
     *
     * 정산 요청에 필요한 루트 노드 목록(Mock)을 모델에 담아
     * 정산 요청 폼 페이지를 렌더링합니다.
     *
     * @author gayul.kim
     * @param model Model 객체
     * @return templates/pages/settlement/request.html
     */
    @GetMapping("/request")
    public String requestForm(Model model) {
        model.addAttribute("currentPage", "settlement-request");
        model.addAttribute("pageTitle", "정산 요청");

        // 정산 요청 폼 루트 노드 선택 드롭다운용 Mock 데이터
        model.addAttribute("rootNodes", createMockRootNodes());

        return "pages/settlement/request";
    }

    /**
     * 정산 요청 시 루트 노드 선택을 위한 Mock 리스트
     *
     * @return 루트 노드 Mock 리스트 (id, name, feeRate 포함)
     */
    private List<Map<String, Object>> createMockRootNodes() {
        return List.of(
                Map.of("id", 1L, "name", "본사", "feeRate", "10.00%"),
                Map.of("id", 2L, "name", "서울지사", "feeRate", "5.00%"),
                Map.of("id", 3L, "name", "부산지사", "feeRate", "5.00%"));
    }
}
