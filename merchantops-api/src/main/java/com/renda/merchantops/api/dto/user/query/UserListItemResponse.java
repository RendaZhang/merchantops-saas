package com.renda.merchantops.api.dto.user.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "Tenant-scoped user list item")
public class UserListItemResponse {

    @Schema(description = "User ID", example = "1")
    private Long id;

    @Schema(description = "Username", example = "admin")
    private String username;

    @Schema(description = "Display name", example = "Demo Admin")
    private String displayName;

    @Schema(description = "Email", example = "admin@demo-shop.local")
    private String email;

    @Schema(description = "Status", example = "ACTIVE")
    private String status;
}
