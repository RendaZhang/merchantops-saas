package com.renda.merchantops.api.dto.role.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
@Schema(description = "Tenant role list response")
public class RoleListResponse {

    @Schema(description = "Assignable roles in current tenant")
    private List<RoleListItemResponse> items;
}
