package com.renda.merchantops.api.dto.user.command;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@Schema(description = "Created user result within current tenant")
public class UserCreateResponse {

    @Schema(description = "User ID", example = "5")
    private Long id;

    @Schema(description = "Tenant ID", example = "1")
    private Long tenantId;

    @Schema(description = "Username", example = "cashier")
    private String username;

    @Schema(description = "Display name", example = "Cashier User")
    private String displayName;

    @Schema(description = "Email", example = "cashier@demo-shop.local")
    private String email;

    @Schema(description = "Status", example = "ACTIVE")
    private String status;

    @ArraySchema(schema = @Schema(description = "Bound role code", example = "READ_ONLY"))
    private List<String> roleCodes;

    @Schema(description = "Created time", example = "2026-03-10T11:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "Last updated time", example = "2026-03-10T11:00:00")
    private LocalDateTime updatedAt;
}
