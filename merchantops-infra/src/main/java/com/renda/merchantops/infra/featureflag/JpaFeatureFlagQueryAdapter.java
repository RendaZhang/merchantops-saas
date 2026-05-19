package com.renda.merchantops.infra.featureflag;

import com.renda.merchantops.domain.featureflag.FeatureFlagItem;
import com.renda.merchantops.domain.featureflag.FeatureFlagQueryPort;
import com.renda.merchantops.infra.persistence.entity.FeatureFlagEntity;
import com.renda.merchantops.infra.repository.FeatureFlagRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class JpaFeatureFlagQueryAdapter implements FeatureFlagQueryPort {

    private final FeatureFlagRepository featureFlagRepository;

    public JpaFeatureFlagQueryAdapter(FeatureFlagRepository featureFlagRepository) {
        this.featureFlagRepository = featureFlagRepository;
    }

    @Override
    public List<FeatureFlagItem> listFlags(Long tenantId) {
        return featureFlagRepository.findAllByTenantIdOrderByFlagKeyAsc(tenantId).stream()
                .map(this::toItem)
                .toList();
    }

    @Override
    public Optional<FeatureFlagItem> findByKey(Long tenantId, String key) {
        return featureFlagRepository.findByTenantIdAndFlagKey(tenantId, key).map(this::toItem);
    }

    private FeatureFlagItem toItem(FeatureFlagEntity entity) {
        return new FeatureFlagItem(
                entity.getId(),
                entity.getTenantId(),
                entity.getFlagKey(),
                entity.isEnabled(),
                entity.getUpdatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
