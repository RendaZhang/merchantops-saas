package com.renda.merchantops.api.dto.role.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "Assignable role in current tenant")
public class RoleListItemResponse {

    @Schema(description = "Role id", example = "11")
    private Long id;

    @Schema(description = "Role code", example = "TENANT_ADMIN")
    private String roleCode;

    @Schema(description = "Role name", example = "Tenant Admin")
    private String roleName;
}
