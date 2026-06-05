package com.renda.merchantops.api.dto.featureflag.query;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Feature flag state")
public record FeatureFlagItemResponse(
        @Schema(description = "Feature flag row id", example = "1")
        Long id,
        @Schema(description = "Stable feature flag key", example = "ai.ticket.summary.enabled")
        String key,
        @Schema(description = "Whether the capability is enabled", example = "true")
        boolean enabled,
        @Schema(description = "Last update timestamp", example = "2026-04-06T13:30:00")
        LocalDateTime updatedAt
) {
}
