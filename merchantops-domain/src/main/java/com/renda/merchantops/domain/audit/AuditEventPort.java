package com.renda.merchantops.domain.audit;

import java.util.List;

public interface AuditEventPort {

    void save(NewAuditEvent event);

    List<AuditEventItem> findAllByEntity(Long tenantId, String entityType, Long entityId);
}
