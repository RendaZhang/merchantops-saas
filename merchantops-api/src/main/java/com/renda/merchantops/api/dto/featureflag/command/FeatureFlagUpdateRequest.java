package com.renda.merchantops.api.dto.featureflag.command;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Feature flag update request")
public class FeatureFlagUpdateRequest {

    @Schema(
            description = "Target enabled state for the feature flag",
            example = "true",
            requiredMode = Schema.RequiredMode.REQUIRED,
            nullable = false
    )
    private Boolean enabled;
}
