package com.renda.merchantops.domain.featureflag;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public class FeatureFlagQueryService implements FeatureFlagQueryUseCase {

    private static final Set<String> KNOWN_KEYS = Set.copyOf(FeatureFlagKey.orderedKeys());

    private final FeatureFlagQueryPort featureFlagQueryPort;

    public FeatureFlagQueryService(FeatureFlagQueryPort featureFlagQueryPort) {
        this.featureFlagQueryPort = featureFlagQueryPort;
    }

    @Override
    public List<FeatureFlagItem> listFlags(Long tenantId) {
        Long resolvedTenantId = requireTenantId(tenantId);
        return featureFlagQueryPort.listFlags(resolvedTenantId).stream()
                .filter(item -> item != null && KNOWN_KEYS.contains(item.key()))
                .sorted((left, right) -> left.key().compareTo(right.key()))
                .toList();
    }

    @Override
    public Optional<FeatureFlagItem> findByKey(Long tenantId, String key) {
        Long resolvedTenantId = requireTenantId(tenantId);
        return FeatureFlagKey.fromKey(key)
                .flatMap(item -> featureFlagQueryPort.findByKey(resolvedTenantId, item.key()));
    }

    private Long requireTenantId(Long tenantId) {
        if (tenantId == null) {
            throw new com.renda.merchantops.domain.shared.error.BizException(
                    com.renda.merchantops.domain.shared.error.ErrorCode.UNAUTHORIZED,
                    "tenant context missing"
            );
        }
        return tenantId;
    }
}
