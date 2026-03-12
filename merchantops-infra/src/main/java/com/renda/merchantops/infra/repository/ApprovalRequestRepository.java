package com.renda.merchantops.infra.repository;

import com.renda.merchantops.infra.persistence.entity.ApprovalRequestEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequestEntity, Long> {

    Optional<ApprovalRequestEntity> findByIdAndTenantId(Long id, Long tenantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from ApprovalRequestEntity a where a.id = :id and a.tenantId = :tenantId")
    Optional<ApprovalRequestEntity> findByIdAndTenantIdForUpdate(@Param("id") Long id, @Param("tenantId") Long tenantId);

    boolean existsByTenantIdAndActionTypeAndEntityTypeAndEntityIdAndStatus(Long tenantId,
                                                                            String actionType,
                                                                            String entityType,
                                                                            Long entityId,
                                                                            String status);
}
