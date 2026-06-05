package com.renda.merchantops.api.featureflag;

import com.renda.merchantops.api.dto.featureflag.query.FeatureFlagItemResponse;
import com.renda.merchantops.api.dto.featureflag.query.FeatureFlagListResponse;
import com.renda.merchantops.domain.featureflag.FeatureFlagItem;
import com.renda.merchantops.domain.featureflag.FeatureFlagQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

    @Service
@RequiredArgsConstructor
public class FeatureFlagQueryService {

    private final FeatureFlagQueryUseCase featureFlagQueryUseCase;

    public FeatureFlagListResponse listFlags(Long tenantId) {
        return new FeatureFlagListResponse(
                featureFlagQueryUseCase.listFlags(tenantId).stream()
                        .map(this::toResponse)
                        .toList()
        );
    }

    FeatureFlagItemResponse toResponse(FeatureFlagItem item) {
        return new FeatureFlagItemResponse(
                item.id(),
                item.key(),
                item.enabled(),
                item.updatedAt()
        );
    }
}
