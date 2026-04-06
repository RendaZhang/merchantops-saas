package com.renda.merchantops.domain.featureflag;

import java.time.LocalDateTime;

public record FeatureFlagWriteResult(
        Long id,
        Long tenantId,
        String key,
        boolean enabled,
        Long updatedBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
