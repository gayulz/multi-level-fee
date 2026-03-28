package com.example.settlement.service;

import com.example.settlement.domain.entity.Organization;
import com.example.settlement.domain.entity.enums.OrgType;

import java.util.List;

/**
 * [NEW] 조직 관리 서비스 인터페이스
 *
 * @author gayul.kim
 * @since 2026-03-09
 */
public interface OrganizationService {
    Organization createOrganization(String name, OrgType type, Long parentId);

    Organization getOrganization(Long id);

    List<Organization> getAllOrganizations();

    List<Organization> getRootOrganizations();

    List<Organization> getChildOrganizations(Long parentId);

    void updateOrganization(Long id, String name);

    void deleteOrganization(Long id);
}
