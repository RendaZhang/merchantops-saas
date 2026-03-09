package com.renda.merchantops.api.controller;

import com.renda.merchantops.api.dto.user.UserSummaryResponse;
import com.renda.merchantops.api.security.RequirePermission;
import com.renda.merchantops.api.service.UserQueryService;
import com.renda.merchantops.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "User")
public class UserManagementController {

    private final UserQueryService userQueryService;

    @Operation(summary = "List users in current tenant")
    @RequirePermission("USER_READ")
    @GetMapping()
    public ApiResponse<List<UserSummaryResponse>> listUsers() {
        return ApiResponse.success(userQueryService.listCurrentTenantUsers());
    }

}
