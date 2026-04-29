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
        FeatureFlagKey resolvedFeatureFlag = FeatureFlagKey.fromKey(key)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "feature flag not found"));
        String resolvedKey = resolvedFeatureFlag.key();
        boolean enabled = requireEnabled(command == null ? null : command.enabled());
        ManagedFeatureFlag current = featureFlagCommandPort.findByKeyForUpdate(resolvedTenantId, resolvedKey)
                .orElseGet(() -> resolvedFeatureFlag.defaultManagedFlag(resolvedTenantId));

        if (current.enabled() == enabled) {
            return FeatureFlagWriteResult.noChange(toItem(current));
        }

        LocalDateTime now = LocalDateTime.now();
        ManagedFeatureFlag saved = featureFlagCommandPort.save(new ManagedFeatureFlag(
                current.id(),
                current.tenantId(),
                current.key(),
                enabled,
                resolvedOperatorId,
                current.createdAt() == null ? now : current.createdAt(),
                now
        ));
        return FeatureFlagWriteResult.mutated(toItem(current), toItem(saved));
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

    private FeatureFlagItem toItem(ManagedFeatureFlag featureFlag) {
        return new FeatureFlagItem(
                featureFlag.id(),
                featureFlag.tenantId(),
                featureFlag.key(),
                featureFlag.enabled(),
                featureFlag.updatedBy(),
                featureFlag.createdAt(),
                featureFlag.updatedAt()
        );
    }
}
