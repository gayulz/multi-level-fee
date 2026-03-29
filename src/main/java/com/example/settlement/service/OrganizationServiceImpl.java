package com.example.settlement.service;

import com.example.settlement.domain.entity.Organization;
import com.example.settlement.domain.entity.enums.OrgType;
import com.example.settlement.domain.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * [NEW] 조직 관리 서비스 구현체
 *
 * @author gayul.kim
 * @since 2026-03-09
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrganizationServiceImpl implements OrganizationService {

    private final OrganizationRepository organizationRepository;

    @Override
    @Transactional
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Organization createOrganization(String name, OrgType type, Long parentId) {
        Organization parent = null;
        if (parentId != null) {
            parent = organizationRepository.findById(parentId)
                    .orElseThrow(() -> new IllegalArgumentException("부모 조직을 찾을 수 없습니다"));
        }

        String orgCode = type.name() + "-" + System.currentTimeMillis();
        Organization org;

        if (type == OrgType.HEADQUARTERS) {
            org = Organization.createHeadquarters(name, orgCode);
        } else if (type == OrgType.BRANCH) {
            org = Organization.createBranch(name, orgCode, parent);
        } else {
            org = Organization.createAgency(name, orgCode, parent);
        }

        return organizationRepository.save(org);
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public Organization getOrganization(Long id) {
        return organizationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("조직을 찾을 수 없습니다: id=" + id));
    }

    @Override
    public List<Organization> getAllOrganizations() {
        return organizationRepository.findAll();
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public List<Organization> getRootOrganizations() {
        return organizationRepository.findByParentIsNull()
                .map(List::of)  // Optional<Organization>을 List로 변환
                .orElse(List.of());  // 없으면 빈 리스트 반환
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public List<Organization> getChildOrganizations(Long parentId) {
        Organization parent = getOrganization(parentId);
        return parent.getChildren();
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public void updateOrganization(Long id, String name) {
        Organization org = getOrganization(id);
        org.updateOrgName(name);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public void deleteOrganization(Long id) {
        Organization org = getOrganization(id);
        if (!org.getChildren().isEmpty()) {
            throw new IllegalStateException("하위 조직이 존재하여 삭제할 수 없습니다.");
        }
        organizationRepository.delete(org);
    }
}
