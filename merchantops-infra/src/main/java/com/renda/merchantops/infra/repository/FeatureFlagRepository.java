package com.renda.merchantops.infra.repository;

import com.renda.merchantops.infra.persistence.entity.FeatureFlagEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FeatureFlagRepository extends JpaRepository<FeatureFlagEntity, Long> {

    List<FeatureFlagEntity> findAllByTenantIdOrderByFlagKeyAsc(Long tenantId);

    Optional<FeatureFlagEntity> findByTenantIdAndFlagKey(Long tenantId, String flagKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select f from FeatureFlagEntity f
            where f.tenantId = :tenantId
              and f.flagKey = :flagKey
            """)
    Optional<FeatureFlagEntity> findByTenantIdAndFlagKeyForUpdate(@Param("tenantId") Long tenantId,
                                                                  @Param("flagKey") String flagKey);
}
