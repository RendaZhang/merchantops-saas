package com.renda.merchantops.api.dto.rbac;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "RBAC action execution result")
public class RbacActionResponse {

    @Schema(description = "Attempted action", example = "read users")
    private String action;

    @Schema(description = "Authorization result", example = "allowed")
    private String result;
}
