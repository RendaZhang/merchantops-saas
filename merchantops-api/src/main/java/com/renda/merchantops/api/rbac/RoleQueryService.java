package com.renda.merchantops.api.rbac;

import com.renda.merchantops.api.dto.role.query.RoleListItemResponse;
import com.renda.merchantops.api.dto.role.query.RoleListResponse;
import com.renda.merchantops.domain.user.RoleQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoleQueryService {

    private final RoleQueryUseCase roleQueryUseCase;

    public RoleListResponse listRoles(Long tenantId) {
        return new RoleListResponse(
                roleQueryUseCase.listRoles(tenantId).stream()
                        .map(role -> new RoleListItemResponse(
                                role.id(),
                                role.roleCode(),
                                role.roleName()
                        ))
                        .toList()
        );
    }
}
