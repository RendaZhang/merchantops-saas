package com.renda.merchantops.api.controller;

import com.renda.merchantops.api.security.RequirePermission;
import com.renda.merchantops.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/rbac")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "RBAC Test")
public class RbacDemoController {

    @Operation(summary = "Requires USER_READ permission")
    @RequirePermission("USER_READ")
    @GetMapping("/users")
    public ApiResponse<Map<String, Object>> readUsers() {
        return ApiResponse.success(Map.of(
                "action", "read users",
                "result", "allowed"
        ));
    }

    @Operation(summary = "Requires USER_WRITE permission")
    @RequirePermission("USER_WRITE")
    @GetMapping("/users/manage")
    public ApiResponse<Map<String, Object>> manageUsers() {
        return ApiResponse.success(Map.of(
                "action", "manage users",
                "result", "allowed"
        ));
    }

    @Operation(summary = "Requires FEATURE_FLAG_MANAGE permission")
    @RequirePermission("FEATURE_FLAG_MANAGE")
    @GetMapping("/feature-flags")
    public ApiResponse<Map<String, Object>> manageFeatureFlags() {
        return ApiResponse.success(Map.of(
                "action", "manage feature flags",
                "result", "allowed"
        ));
    }

}
