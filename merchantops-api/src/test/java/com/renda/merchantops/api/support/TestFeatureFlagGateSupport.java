package com.renda.merchantops.api.support;

import com.renda.merchantops.api.featureflag.FeatureFlagGateService;
import com.renda.merchantops.domain.featureflag.FeatureFlagKey;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class TestFeatureFlagGateSupport {

    private TestFeatureFlagGateSupport() {
    }

    public static FeatureFlagGateService alwaysEnabledGateService() {
        FeatureFlagGateService gateService = mock(FeatureFlagGateService.class);
        when(gateService.isEnabled(anyLong(), any(FeatureFlagKey.class))).thenReturn(true);
        return gateService;
    }
}
