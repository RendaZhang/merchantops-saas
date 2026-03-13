package com.renda.merchantops.api.dto.user.command;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@Schema(description = "User role assignment result")
public class UserRoleAssignmentResponse {

    @Schema(description = "User id", example = "3")
    private Long id;

    @Schema(description = "Tenant id", example = "1")
    private Long tenantId;

    @Schema(description = "Username", example = "viewer")
    private String username;

    @ArraySchema(schema = @Schema(description = "Current role code", example = "TENANT_ADMIN"))
    private List<String> roleCodes;

    @ArraySchema(schema = @Schema(description = "Current permission code", example = "USER_WRITE"))
    private List<String> permissionCodes;

    @Schema(description = "Last update time", example = "2026-03-10T18:00:00")
    private LocalDateTime updatedAt;
}
