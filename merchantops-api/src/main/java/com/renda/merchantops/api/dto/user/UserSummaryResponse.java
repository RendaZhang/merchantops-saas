package com.renda.merchantops.api.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "Tenant user summary")
public class UserSummaryResponse {

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
