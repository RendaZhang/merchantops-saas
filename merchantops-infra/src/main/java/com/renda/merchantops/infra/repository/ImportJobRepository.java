package com.renda.merchantops.infra.repository;

import com.renda.merchantops.infra.persistence.entity.ImportJobEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ImportJobRepository extends JpaRepository<ImportJobEntity, Long> {

    Page<ImportJobEntity> findAllByTenantId(Long tenantId, Pageable pageable);

    Optional<ImportJobEntity> findByIdAndTenantId(Long id, Long tenantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT j
            FROM ImportJobEntity j
            WHERE j.id = :id
              AND j.tenantId = :tenantId
            """)
    Optional<ImportJobEntity> findByIdAndTenantIdForUpdate(@Param("id") Long id, @Param("tenantId") Long tenantId);
}
