package com.renda.merchantops.infra.repository;

import com.renda.merchantops.infra.persistence.entity.ImportJobEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ImportJobRepository extends JpaRepository<ImportJobEntity, Long> {

    @Query("""
            select j from ImportJobEntity j
            where j.tenantId = :tenantId
              and (:status is null or j.status = :status)
              and (:importType is null or j.importType = :importType)
              and (:requestedBy is null or j.requestedBy = :requestedBy)
              and (:hasFailuresOnly = false or j.failureCount > 0)
            """)
    Page<ImportJobEntity> searchPageByTenantId(@Param("tenantId") Long tenantId,
                                                @Param("status") String status,
                                                @Param("importType") String importType,
                                                @Param("requestedBy") Long requestedBy,
                                                @Param("hasFailuresOnly") boolean hasFailuresOnly,
                                                Pageable pageable);

    Optional<ImportJobEntity> findByIdAndTenantId(Long id, Long tenantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT j
            FROM ImportJobEntity j
            WHERE j.id = :id
              AND j.tenantId = :tenantId
            """)
    Optional<ImportJobEntity> findByIdAndTenantIdForUpdate(@Param("id") Long id, @Param("tenantId") Long tenantId);

    @Query("""
            SELECT j
            FROM ImportJobEntity j
            WHERE j.status = :status
              AND j.createdAt <= :createdBefore
            ORDER BY j.createdAt ASC, j.id ASC
            """)
    List<ImportJobEntity> findQueuedJobsForEnqueueRecovery(@Param("status") String status,
                                                           @Param("createdBefore") LocalDateTime createdBefore,
                                                           Pageable pageable);
}
