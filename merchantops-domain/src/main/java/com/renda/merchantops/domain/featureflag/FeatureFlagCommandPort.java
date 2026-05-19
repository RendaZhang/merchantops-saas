package com.renda.merchantops.domain.featureflag;

import java.util.Optional;

public interface FeatureFlagCommandPort {

    Optional<ManagedFeatureFlag> findByKeyForUpdate(Long tenantId, String key);

    ManagedFeatureFlag save(ManagedFeatureFlag featureFlag);
}
