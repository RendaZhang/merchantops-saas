package com.renda.merchantops.api.service;

import com.renda.merchantops.api.dto.role.query.RoleListItemResponse;
import com.renda.merchantops.api.dto.role.query.RoleListResponse;
import com.renda.merchantops.infra.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoleQueryService {

    private final RoleRepository roleRepository;

    public RoleListResponse listRoles(Long tenantId) {
        return new RoleListResponse(
                roleRepository.findAllByTenantIdOrderByIdAsc(tenantId).stream()
                        .map(role -> new RoleListItemResponse(
                                role.getId(),
                                role.getRoleCode(),
                                role.getRoleName()
                        ))
                        .toList()
        );
    }
}
