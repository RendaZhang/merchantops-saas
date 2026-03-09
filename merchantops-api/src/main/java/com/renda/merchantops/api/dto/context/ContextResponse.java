package com.renda.merchantops.api.dto.context;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "Current tenant and user context extracted from JWT")
public class ContextResponse {

    @Schema(description = "Current tenant ID", example = "1")
    private Long tenantId;

    @Schema(description = "Current tenant code", example = "demo-shop")
    private String tenantCode;

    @Schema(description = "Current user ID", example = "1")
    private Long userId;

    @Schema(description = "Current username", example = "admin")
    private String username;
}
