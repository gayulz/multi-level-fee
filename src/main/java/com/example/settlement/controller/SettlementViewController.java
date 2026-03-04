package com.example.settlement.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * [NEW] 정산 요청/결과 뷰 컨트롤러
 *
 * 정산 요청 폼, 정산 내역 목록, 정산 상세 페이지 렌더링 및 Mock 데이터를 제공합니다.
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
     * [NEW] 정산 내역 목록 페이지
     *
     * Mock 정산 이력 데이터를 모델에 담아
     * 정산 내역 목록 페이지를 렌더링합니다.
     *
     * @author gayul.kim
     * @param model Model 객체
     * @return templates/pages/settlement/history.html
     */
    @GetMapping("/history")
    public String history(Model model) {
        model.addAttribute("currentPage", "settlement-history");
        model.addAttribute("pageTitle", "정산 내역");

        // Mock 정산 내역 데이터
        model.addAttribute("settlements", createMockSettlementHistory());

        return "pages/settlement/history";
    }

    /**
     * [NEW] 정산 상세 결과 페이지
     *
     * 특정 정산 건의 요약 정보와 노드별 수수료 배분 내역을
     * Mock 데이터로 제공합니다.
     *
     * @author gayul.kim
     * @param id    정산 건 ID
     * @param model Model 객체
     * @return templates/pages/settlement/detail.html
     */
    @GetMapping("/detail/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("currentPage", "settlement-history");
        model.addAttribute("pageTitle", "정산 상세");

        // Mock 정산 상세 데이터
        model.addAttribute("settlement", createMockSettlementDetail(id));
        model.addAttribute("feeDetails", createMockFeeDetails());

        return "pages/settlement/detail";
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

    /**
     * Mock 정산 내역 리스트 (5건)
     *
     * @return 정산 내역 Mock 리스트
     */
    private List<Map<String, Object>> createMockSettlementHistory() {
        List<Map<String, Object>> list = new ArrayList<>();
        String[] statuses = { "COMPLETED", "COMPLETED", "COMPLETED", "PENDING", "FAILED" };
        String[] nodeNames = { "본사", "서울지사", "본사", "부산지사", "본사" };
        long[] amounts = { 100_000L, 250_000L, 50_000L, 180_000L, 320_000L };

        for (int i = 1; i <= 5; i++) {
            list.add(Map.of(
                    "id", (long) i,
                    "orderId", String.format("ORD-20260304-%03d", i),
                    "amount", amounts[i - 1],
                    "totalFee", (long) (amounts[i - 1] * 0.1705),
                    "rootNodeName", nodeNames[i - 1],
                    "status", statuses[i - 1],
                    "settledAt", LocalDateTime.now().minusHours(i)));
        }

        return list;
    }

    /**
     * Mock 정산 상세 정보 (단건)
     *
     * @param id 정산 건 ID
     * @return 정산 상세 Mock 데이터
     */
    private Map<String, Object> createMockSettlementDetail(Long id) {
        return Map.of(
                "id", id,
                "orderId", String.format("ORD-20260304-%03d", id),
                "amount", 100_000L,
                "totalFee", 17_050L,
                "rootNodeName", "본사",
                "status", "COMPLETED",
                "settledAt", LocalDateTime.now().minusHours(1),
                "remark", "정상 처리 완료");
    }

    /**
     * Mock 노드별 수수료 배분 내역
     *
     * @return 수수료 배분 Mock 리스트 (트리 구조 표현용 depth 포함)
     */
    private List<Map<String, Object>> createMockFeeDetails() {
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(Map.of("nodeName", "본사", "depth", 0, "rate", "10.00%", "fee", 10_000L));
        list.add(Map.of("nodeName", "서울지사", "depth", 1, "rate", "5.00%", "fee", 5_000L));
        list.add(Map.of("nodeName", "강남대리점", "depth", 2, "rate", "3.00%", "fee", 3_000L));
        list.add(Map.of("nodeName", "종로대리점", "depth", 2, "rate", "2.05%", "fee", 2_050L));
        return list;
    }
}
