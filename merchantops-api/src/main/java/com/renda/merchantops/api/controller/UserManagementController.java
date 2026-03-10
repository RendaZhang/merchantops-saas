package com.renda.merchantops.api.controller;

import com.renda.merchantops.api.context.ContextAccess;
import com.renda.merchantops.api.contract.UserManagementApi;
import com.renda.merchantops.api.dto.user.query.UserPageQuery;
import com.renda.merchantops.api.dto.user.query.UserPageResponse;
import com.renda.merchantops.api.security.RequirePermission;
import com.renda.merchantops.api.service.UserQueryService;
import com.renda.merchantops.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserManagementController implements UserManagementApi {

    private final UserQueryService userQueryService;

    @Override
    @RequirePermission("USER_READ")
    public ApiResponse<UserPageResponse> listUsers(UserPageQuery query) {
        Long tenantId = ContextAccess.requireTenantId();
        return ApiResponse.success(userQueryService.pageUsers(tenantId, query));
    }
}
