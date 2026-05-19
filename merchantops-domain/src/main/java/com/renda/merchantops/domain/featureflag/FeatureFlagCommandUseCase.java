package com.renda.merchantops.domain.featureflag;

public interface FeatureFlagCommandUseCase {

    FeatureFlagWriteResult updateFlag(Long tenantId, Long operatorId, String key, UpdateFeatureFlagCommand command);
}
