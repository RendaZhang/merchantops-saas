package com.renda.merchantops.api.dto.featureflag.command;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeatureFlagUpdateRequest {

    @NotNull
    private Boolean enabled;
}
