package com.renda.merchantops.api.dto.featureflag.query;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Feature flag list")
public record FeatureFlagListResponse(
        @Schema(description = "Stable feature flag rows in key order")
        List<FeatureFlagItemResponse> items
) {
}
