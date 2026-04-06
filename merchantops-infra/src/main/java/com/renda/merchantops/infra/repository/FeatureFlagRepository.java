package com.renda.merchantops.infra.repository;

import com.renda.merchantops.infra.persistence.entity.FeatureFlagEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FeatureFlagRepository extends JpaRepository<FeatureFlagEntity, Long> {

    List<FeatureFlagEntity> findAllByTenantIdOrderByFlagKeyAsc(Long tenantId);

    Optional<FeatureFlagEntity> findByTenantIdAndFlagKey(Long tenantId, String flagKey);
}
