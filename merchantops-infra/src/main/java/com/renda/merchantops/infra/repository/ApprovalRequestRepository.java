package com.renda.merchantops.infra.repository;

import com.renda.merchantops.infra.persistence.entity.ApprovalRequestEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Set;

public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequestEntity, Long> {

    Optional<ApprovalRequestEntity> findByIdAndTenantId(Long id, Long tenantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from ApprovalRequestEntity a where a.id = :id and a.tenantId = :tenantId")
    Optional<ApprovalRequestEntity> findByIdAndTenantIdForUpdate(@Param("id") Long id, @Param("tenantId") Long tenantId);

    @Query("""
            select a from ApprovalRequestEntity a
            where a.tenantId = :tenantId
              and (:status is null or a.status = :status)
              and (:actionType is null or a.actionType = :actionType)
              and (:requestedBy is null or a.requestedBy = :requestedBy)
              and a.actionType in :allowedActionTypes
            """)
    Page<ApprovalRequestEntity> searchPageByTenantId(@Param("tenantId") Long tenantId,
                                                      @Param("status") String status,
                                                      @Param("actionType") String actionType,
                                                      @Param("requestedBy") Long requestedBy,
                                                      @Param("allowedActionTypes") Set<String> allowedActionTypes,
                                                      Pageable pageable);
}
