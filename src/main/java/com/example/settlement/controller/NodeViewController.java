package com.example.settlement.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * [NEW] 정산 노드 관리 컨트롤러
 *
 * 노드 트리 뷰 렌더링 및 등록/수정 폼 페이지 렌더링을 담당하며,
 * UI 검증을 위해 jsTree용 Mock 데이터를 제공합니다.
 *
 * @author gayul.kim
 * @since 2026-02-25
 */
@Controller
@RequestMapping("/nodes")
public class NodeViewController {

    /**
     * 정산 노드 목록 (트리 뷰)
     *
     * @param model Model 객체
     * @return templates/pages/nodes/list.html
     */
    @GetMapping
    public String list(Model model) {
        model.addAttribute("currentPage", "nodes");
        model.addAttribute("pageTitle", "노드 관리");

        // jsTree용 Mock 데이터 생성
        model.addAttribute("treeData", createMockTreeData());

        return "pages/nodes/list";
    }

    /**
     * 정산 노드 등록 폼
     *
     * @param model Model 객체
     * @return templates/pages/nodes/form.html
     */
    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("currentPage", "nodes");
        model.addAttribute("pageTitle", "노드 등록");

        // 등록 폼 상위 노드 선택용 Mock 리스트
        model.addAttribute("parentNodes", createMockParentNodes());

        return "pages/nodes/form";
    }

    /**
     * 정산 노드 수정 폼 (Mock)
     *
     * @param model Model 객체
     * @return templates/pages/nodes/form.html
     */
    @GetMapping("/edit")
    public String editForm(Model model) {
        model.addAttribute("currentPage", "nodes");
        model.addAttribute("pageTitle", "노드 수정");
        model.addAttribute("parentNodes", createMockParentNodes());

        return "pages/nodes/form";
    }

    /**
     * jsTree 렌더링용 Mock 데이터
     * jsTree Format: { id: "string", parent: "string" (루트는 "#"), text: "string" }
     */
    private List<Map<String, Object>> createMockTreeData() {
        List<Map<String, Object>> list = new ArrayList<>();

        // Root Node
        list.add(Map.of("id", "1", "parent", "#", "text", "본사 (10.00%)", "state", Map.of("opened", true)));

        // Depth 1
        list.add(Map.of("id", "2", "parent", "1", "text", "서울지사 (5.00%)", "state", Map.of("opened", true)));
        list.add(Map.of("id", "3", "parent", "1", "text", "부산지사 (5.00%)"));

        // Depth 2
        list.add(Map.of("id", "4", "parent", "2", "text", "강남대리점 (3.00%)"));
        list.add(Map.of("id", "5", "parent", "2", "text", "종로대리점 (3.00%)"));

        list.add(Map.of("id", "6", "parent", "3", "text", "서면대리점 (3.00%)"));

        return list;
    }

    /**
     * 폼에서 상위 노드 선택을 위한 Mock 리스트
     */
    private List<Map<String, Object>> createMockParentNodes() {
        return List.of(
                Map.of("id", 1L, "name", "본사"),
                Map.of("id", 2L, "name", "서울지사"),
                Map.of("id", 3L, "name", "부산지사"));
    }
}
