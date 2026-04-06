package com.example.settlement.controller;

import com.example.settlement.domain.repository.SettlementNodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * [NEW] 정산 노드 관리 컨트롤러
 *
 * 노드 트리 뷰 렌더링 및 등록/수정 폼 페이지 렌더링을 담당하며,
 * UI 검증을 위해 jsTree용 데이터와 생성 폼 리스트를 제공합니다.
 *
 * @author gayul.kim
 * @since 2026-02-25
 */
@Controller
@RequestMapping("/nodes")
@RequiredArgsConstructor
public class NodeViewController {

    private final SettlementNodeRepository settlementNodeRepository;

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

        // DB에서 최신 노드 데이터를 조회하여 jsTree 렌더링 포맷으로 변환 전달
        model.addAttribute("treeData", getTreeData());

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

        // 실제 하위 노드를 생성할 수 있는 후보(본사, 지사) 노드만 리스트업
        model.addAttribute("parentNodes", getAvailableParentNodes());

        return "pages/nodes/form";
    }

    /**
     * 정산 노드 수정 폼
     *
     * @param model Model 객체
     * @return templates/pages/nodes/form.html
     */
    @GetMapping("/edit")
    public String editForm(Model model) {
        model.addAttribute("currentPage", "nodes");
        model.addAttribute("pageTitle", "노드 수정");
        model.addAttribute("parentNodes", getAvailableParentNodes());

        return "pages/nodes/form";
    }

    /**
     * 하위 노드 생성이 가능한 부모 노드 목록 조회
     */
    private List<Map<String, Object>> getAvailableParentNodes() {
        return settlementNodeRepository.findAll().stream()
                .filter(node -> node.getOrganization() != null && node.getOrganization().getLevel() < 3)
                .map(node -> Map.<String, Object>of(
                        "id", node.getId(),
                        "name", node.getName()
                ))
                .collect(Collectors.toList());
    }

    /**
     * DB의 정산 노드 모델을 jsTree 렌더링용 데이터 포맷으로 변환
     * Format: { id: "string", parent: "string" (루트는 "#"), text: "string", state: {opened: boolean} }
     */
    private List<Map<String, Object>> getTreeData() {
        return settlementNodeRepository.findAll().stream()
                .map(node -> {
                    String parentId = (node.getParent() != null) ? node.getParent().getId().toString() : "#";
                    String text = String.format("%s (%.2f%%)", node.getName(), node.getFeeRate());

                    // 본사 및 지사 레벨의 노드 트리는 기본적으로 펼침(opened) 상태로 표시
                    boolean isOpened = (node.getOrganization() != null && node.getOrganization().getLevel() <= 2);

                    return Map.<String, Object>of(
                            "id", node.getId().toString(),
                            "parent", parentId,
                            "text", text,
                            "state", Map.of("opened", isOpened)
                    );
                })
                .collect(Collectors.toList());
    }
}
