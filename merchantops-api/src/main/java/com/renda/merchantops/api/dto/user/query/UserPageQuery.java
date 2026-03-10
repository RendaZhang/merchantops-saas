package com.renda.merchantops.api.dto.user.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User page query within current tenant")
public class UserPageQuery {

    @Schema(description = "Zero-based page index", example = "0", defaultValue = "0")
    private Integer page = 0;

    @Schema(description = "Page size", example = "10", defaultValue = "10")
    private Integer size = 10;

    @Schema(description = "Username fuzzy filter", example = "ad")
    private String username;

    @Schema(description = "Exact status filter", example = "ACTIVE")
    private String status;

    @Schema(description = "Exact role code filter", example = "TENANT_ADMIN")
    private String roleCode;
}
