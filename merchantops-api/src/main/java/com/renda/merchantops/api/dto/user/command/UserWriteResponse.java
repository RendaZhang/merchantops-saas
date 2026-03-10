package com.renda.merchantops.api.dto.user.command;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@Schema(description = "User write result within current tenant")
public class UserWriteResponse {

    @Schema(description = "User ID", example = "5")
    private Long id;

    @Schema(description = "Tenant ID", example = "1")
    private Long tenantId;

    @Schema(description = "Username", example = "cashier")
    private String username;

    @Schema(description = "Display name", example = "Updated Cashier")
    private String displayName;

    @Schema(description = "Email", example = "cashier.updated@demo-shop.local")
    private String email;

    @Schema(description = "Status", example = "ACTIVE")
    private String status;

    @Schema(description = "Last updated time", example = "2026-03-10T14:00:00")
    private LocalDateTime updatedAt;
}
