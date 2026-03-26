package com.renda.merchantops.api.rbac;

import com.renda.merchantops.api.context.ContextAccess;
import com.renda.merchantops.api.rbac.RoleManagementApi;
import com.renda.merchantops.api.dto.role.query.RoleListResponse;
import com.renda.merchantops.api.security.RequirePermission;
import com.renda.merchantops.api.rbac.RoleQueryService;
import com.renda.merchantops.api.platform.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RoleController implements RoleManagementApi {

    private final RoleQueryService roleQueryService;

    @Override
    @RequirePermission("USER_WRITE")
    public ApiResponse<RoleListResponse> listRoles() {
        Long tenantId = ContextAccess.requireTenantId();
        return ApiResponse.success(roleQueryService.listRoles(tenantId));
    }
}
