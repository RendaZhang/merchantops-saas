package com.renda.merchantops.api.dto.user.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@Schema(description = "Tenant-scoped user detail")
public class UserDetailResponse {

    @Schema(description = "User ID", example = "1")
    private Long id;

    @Schema(description = "Tenant ID", example = "1")
    private Long tenantId;

    @Schema(description = "Username", example = "admin")
    private String username;

    @Schema(description = "Display name", example = "Demo Admin")
    private String displayName;

    @Schema(description = "Email", example = "admin@demo-shop.local")
    private String email;

    @Schema(description = "Status", example = "ACTIVE")
    private String status;

    @Schema(description = "Created time", example = "2026-03-10T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "Last updated time", example = "2026-03-10T10:30:00")
    private LocalDateTime updatedAt;
}
