package com.renda.merchantops.api.dto.dev;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "Development ping response")
public class DevPingResponse {

    @Schema(description = "Service status", example = "UP")
    private String status;

    @Schema(description = "Module name", example = "merchantops-api")
    private String module;
}
