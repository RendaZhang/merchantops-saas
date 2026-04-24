package com.renda.merchantops.domain.featureflag;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FeatureFlagQueryService implements FeatureFlagQueryUseCase {

    private static final List<FeatureFlagKey> ORDERED_KEYS = FeatureFlagKey.orderedValues();
    private static final Set<String> KNOWN_KEYS = ORDERED_KEYS.stream()
            .map(FeatureFlagKey::key)
            .collect(Collectors.toUnmodifiableSet());

    private final FeatureFlagQueryPort featureFlagQueryPort;

    public FeatureFlagQueryService(FeatureFlagQueryPort featureFlagQueryPort) {
        this.featureFlagQueryPort = featureFlagQueryPort;
    }

    @Override
    public List<FeatureFlagItem> listFlags(Long tenantId) {
        Long resolvedTenantId = requireTenantId(tenantId);
        Map<String, FeatureFlagItem> itemsByKey = featureFlagQueryPort.listFlags(resolvedTenantId).stream()
                .filter(item -> item != null && KNOWN_KEYS.contains(item.key()))
                .collect(Collectors.toMap(FeatureFlagItem::key, Function.identity(), (left, right) -> left));

        return ORDERED_KEYS.stream()
                .map(key -> itemsByKey.getOrDefault(key.key(), key.defaultItem(resolvedTenantId)))
                .toList();
    }

    @Override
    public Optional<FeatureFlagItem> findByKey(Long tenantId, String key) {
        Long resolvedTenantId = requireTenantId(tenantId);
        return FeatureFlagKey.fromKey(key)
                .map(item -> featureFlagQueryPort.findByKey(resolvedTenantId, item.key())
                        .orElseGet(() -> item.defaultItem(resolvedTenantId)));
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
