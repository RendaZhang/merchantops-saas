package com.renda.merchantops.api.dto.health;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "Service health response")
public class HealthResponse {

    @Schema(description = "Service health status", example = "UP")
    private String status;

    @Schema(description = "Service name", example = "merchantops-saas")
    private String service;
}
