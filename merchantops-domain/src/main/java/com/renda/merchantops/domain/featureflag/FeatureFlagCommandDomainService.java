package com.renda.merchantops.domain.featureflag;

import com.renda.merchantops.domain.shared.error.BizException;
import com.renda.merchantops.domain.shared.error.ErrorCode;

import java.time.LocalDateTime;

public class FeatureFlagCommandDomainService implements FeatureFlagCommandUseCase {

    private final FeatureFlagCommandPort featureFlagCommandPort;

    public FeatureFlagCommandDomainService(FeatureFlagCommandPort featureFlagCommandPort) {
        this.featureFlagCommandPort = featureFlagCommandPort;
    }

    @Override
    public FeatureFlagWriteResult updateFlag(Long tenantId, Long operatorId, String key, UpdateFeatureFlagCommand command) {
        Long resolvedTenantId = requireTenantId(tenantId);
        Long resolvedOperatorId = requireOperatorId(operatorId);
        String resolvedKey = FeatureFlagKey.fromKey(key)
                .map(FeatureFlagKey::key)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "feature flag not found"));
        boolean enabled = requireEnabled(command == null ? null : command.enabled());
        ManagedFeatureFlag current = featureFlagCommandPort.findByKey(resolvedTenantId, resolvedKey)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "feature flag not found"));

        if (current.enabled() == enabled) {
            return new FeatureFlagWriteResult(
                    current.id(),
                    current.tenantId(),
                    current.key(),
                    current.enabled(),
                    current.updatedBy(),
                    current.createdAt(),
                    current.updatedAt()
            );
        }

        ManagedFeatureFlag saved = featureFlagCommandPort.save(new ManagedFeatureFlag(
                current.id(),
                current.tenantId(),
                current.key(),
                enabled,
                resolvedOperatorId,
                current.createdAt(),
                LocalDateTime.now()
        ));
        return new FeatureFlagWriteResult(
                saved.id(),
                saved.tenantId(),
                saved.key(),
                saved.enabled(),
                saved.updatedBy(),
                saved.createdAt(),
                saved.updatedAt()
        );
    }

    private Long requireTenantId(Long tenantId) {
        if (tenantId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "tenant context missing");
        }
        return tenantId;
    }

    private Long requireOperatorId(Long operatorId) {
        if (operatorId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "user context missing");
        }
        return operatorId;
    }

    private boolean requireEnabled(Boolean enabled) {
        if (enabled == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "enabled must not be null");
        }
        return enabled;
    }
}
