package com.renda.merchantops.api.featureflag;

import com.renda.merchantops.domain.featureflag.FeatureFlagItem;
import com.renda.merchantops.domain.featureflag.FeatureFlagKey;
import com.renda.merchantops.domain.featureflag.FeatureFlagQueryUseCase;
import com.renda.merchantops.domain.shared.error.BizException;
import com.renda.merchantops.domain.shared.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FeatureFlagGateService {

    private final FeatureFlagQueryUseCase featureFlagQueryUseCase;

    public boolean isEnabled(Long tenantId, FeatureFlagKey key) {
        return featureFlagQueryUseCase.findByKey(tenantId, key.key())
                .map(FeatureFlagItem::enabled)
                .orElse(false);
    }

    public void requireEnabled(Long tenantId, FeatureFlagKey key, String disabledMessage) {
        if (!isEnabled(tenantId, key)) {
            throw new BizException(ErrorCode.SERVICE_UNAVAILABLE, disabledMessage);
        }
    }
}
