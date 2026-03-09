package com.renda.merchantops.api.controller;

import com.renda.merchantops.api.contract.UserManagementApi;
import com.renda.merchantops.api.dto.user.UserSummaryResponse;
import com.renda.merchantops.api.security.RequirePermission;
import com.renda.merchantops.api.service.UserQueryService;
import com.renda.merchantops.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class UserManagementController implements UserManagementApi {

    private final UserQueryService userQueryService;

    @Override
    @RequirePermission("USER_READ")
    public ApiResponse<List<UserSummaryResponse>> listUsers() {
        return ApiResponse.success(userQueryService.listCurrentTenantUsers());
    }
}
