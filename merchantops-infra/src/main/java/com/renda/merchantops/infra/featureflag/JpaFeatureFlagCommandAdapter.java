package com.renda.merchantops.infra.featureflag;

import com.renda.merchantops.domain.featureflag.FeatureFlagCommandPort;
import com.renda.merchantops.domain.featureflag.ManagedFeatureFlag;
import com.renda.merchantops.infra.persistence.entity.FeatureFlagEntity;
import com.renda.merchantops.infra.repository.FeatureFlagRepository;
import com.renda.merchantops.infra.repository.TenantRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class JpaFeatureFlagCommandAdapter implements FeatureFlagCommandPort {

    private final FeatureFlagRepository featureFlagRepository;
    private final TenantRepository tenantRepository;
    private final EntityManager entityManager;

    public JpaFeatureFlagCommandAdapter(FeatureFlagRepository featureFlagRepository,
                                        TenantRepository tenantRepository,
                                        EntityManager entityManager) {
        this.featureFlagRepository = featureFlagRepository;
        this.tenantRepository = tenantRepository;
        this.entityManager = entityManager;
    }

    @Override
    public Optional<ManagedFeatureFlag> findByKeyForUpdate(Long tenantId, String key) {
        Optional<ManagedFeatureFlag> lockedFlag = lockFeatureFlag(tenantId, key);
        if (lockedFlag.isPresent()) {
            return lockedFlag;
        }

        tenantRepository.findByIdForUpdate(tenantId)
                .orElseThrow(() -> new IllegalStateException("tenant not found for feature flag update: " + tenantId));

        return lockFeatureFlag(tenantId, key);
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

    private Optional<ManagedFeatureFlag> lockFeatureFlag(Long tenantId, String key) {
        return featureFlagRepository.findByTenantIdAndFlagKeyForUpdate(tenantId, key)
                .map(entity -> {
                    entityManager.refresh(entity, LockModeType.PESSIMISTIC_WRITE);
                    return toManagedFeatureFlag(entity);
                });
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
