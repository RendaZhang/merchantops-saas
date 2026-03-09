package com.renda.merchantops.api.controller;

import com.renda.merchantops.api.context.CurrentUserContext;
import com.renda.merchantops.api.context.TenantContext;
import com.renda.merchantops.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Context Test")
public class ContextController {

    @Operation(summary = "Get current tenant and user context")
    @GetMapping("/api/v1/context")
    public ApiResponse<Map<String, Object>> context() {
        return ApiResponse.success(Map.of(
                "tenantId", TenantContext.getTenantId(),
                "tenantCode", TenantContext.getTenantCode(),
                "userId", CurrentUserContext.getUserId(),
                "username", CurrentUserContext.getUsername()
        ));
    }

}
