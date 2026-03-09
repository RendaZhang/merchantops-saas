package com.renda.merchantops.api.controller;

import com.renda.merchantops.api.security.CurrentUser;
import com.renda.merchantops.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/user")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "User")
public class UserController {

    @Operation(summary = "Get current logged-in user")
    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me(@AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.success(Map.of(
                "userId", currentUser.getUserId(),
                "tenantId", currentUser.getTenantId(),
                "tenantCode", currentUser.getTenantCode(),
                "username", currentUser.getUsername(),
                "roles", currentUser.getRoles(),
                "permissions", currentUser.getPermissions()
        ));
    }

}
