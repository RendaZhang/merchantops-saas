package com.renda.merchantops.infra.featureflag;

import com.renda.merchantops.domain.featureflag.FeatureFlagCommandPort;
import com.renda.merchantops.domain.featureflag.ManagedFeatureFlag;
import com.renda.merchantops.infra.persistence.entity.FeatureFlagEntity;
import com.renda.merchantops.infra.repository.FeatureFlagRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class JpaFeatureFlagCommandAdapter implements FeatureFlagCommandPort {

    private final FeatureFlagRepository featureFlagRepository;
    private final EntityManager entityManager;

    public JpaFeatureFlagCommandAdapter(FeatureFlagRepository featureFlagRepository,
                                        EntityManager entityManager) {
        this.featureFlagRepository = featureFlagRepository;
        this.entityManager = entityManager;
    }

    @Override
    public Optional<ManagedFeatureFlag> findByKeyForUpdate(Long tenantId, String key) {
        return featureFlagRepository.findByTenantIdAndFlagKeyForUpdate(tenantId, key)
                .map(entity -> {
                    entityManager.refresh(entity, LockModeType.PESSIMISTIC_WRITE);
                    return toManagedFeatureFlag(entity);
                });
    }

    @Override
    public ManagedFeatureFlag save(ManagedFeatureFlag featureFlag) {
        FeatureFlagEntity entity = new FeatureFlagEntity();
        entity.setId(featureFlag.id());
        entity.setTenantId(featureFlag.tenantId());
        entity.setFlagKey(featureFlag.key());
        entity.setEnabled(featureFlag.enabled());
        entity.setUpdatedBy(featureFlag.updatedBy());
        entity.setCreatedAt(featureFlag.createdAt());
        entity.setUpdatedAt(featureFlag.updatedAt());
        return toManagedFeatureFlag(featureFlagRepository.save(entity));
    }

    private ManagedFeatureFlag toManagedFeatureFlag(FeatureFlagEntity entity) {
        return new ManagedFeatureFlag(
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
