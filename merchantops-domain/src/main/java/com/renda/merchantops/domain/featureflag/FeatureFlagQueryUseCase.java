package com.renda.merchantops.domain.featureflag;

import java.util.List;
import java.util.Optional;

public interface FeatureFlagQueryUseCase {

    List<FeatureFlagItem> listFlags(Long tenantId);

    Optional<FeatureFlagItem> findByKey(Long tenantId, String key);
}
