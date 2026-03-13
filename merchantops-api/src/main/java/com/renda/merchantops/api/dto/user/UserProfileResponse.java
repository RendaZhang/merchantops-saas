package com.renda.merchantops.api.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
@Schema(description = "Current authenticated user profile")
public class UserProfileResponse {

    @Schema(description = "Current user ID", example = "1")
    private Long userId;

    @Schema(description = "Current tenant ID", example = "1")
    private Long tenantId;

    @Schema(description = "Current tenant code", example = "demo-shop")
    private String tenantCode;

    @Schema(description = "Current username", example = "admin")
    private String username;

    @Schema(description = "Role codes granted to the current user", example = "[\"TENANT_ADMIN\"]")
    private List<String> roles;

    @Schema(description = "Permission codes granted to the current user", example = "[\"USER_READ\",\"USER_WRITE\"]")
    private List<String> permissions;
}
