package com.renda.merchantops.infra.repository;

import com.renda.merchantops.infra.persistence.entity.TenantEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TenantRepository extends JpaRepository<TenantEntity, Long> {

    Optional<TenantEntity> findByTenantCode(String tenantCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select t from TenantEntity t
            where t.id = :tenantId
            """)
    Optional<TenantEntity> findByIdForUpdate(@Param("tenantId") Long tenantId);

}
