package com.renda.merchantops.domain.featureflag;

import java.time.LocalDateTime;

public record FeatureFlagItem(
        Long id,
        Long tenantId,
        String key,
        boolean enabled,
        Long updatedBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
