package com.renda.merchantops.domain.featureflag;

public record FeatureFlagWriteResult(
        FeatureFlagItem before,
        FeatureFlagItem after,
        boolean mutated
) {

    public static FeatureFlagWriteResult noChange(FeatureFlagItem current) {
        return new FeatureFlagWriteResult(current, current, false);
    }

    public static FeatureFlagWriteResult mutated(FeatureFlagItem before, FeatureFlagItem after) {
        return new FeatureFlagWriteResult(before, after, true);
    }
}
