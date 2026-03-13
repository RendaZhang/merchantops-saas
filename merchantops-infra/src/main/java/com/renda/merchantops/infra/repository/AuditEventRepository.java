package com.renda.merchantops.infra.repository;

import com.renda.merchantops.infra.persistence.entity.AuditEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditEventRepository extends JpaRepository<AuditEventEntity, Long> {

    List<AuditEventEntity> findAllByTenantIdAndEntityTypeAndEntityIdOrderByIdAsc(Long tenantId,
                                                                                   String entityType,
                                                                                   Long entityId);
}
