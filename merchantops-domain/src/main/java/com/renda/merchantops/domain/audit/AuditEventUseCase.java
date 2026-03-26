package com.renda.merchantops.domain.audit;

import java.util.List;

public interface AuditEventUseCase {

    void recordEvent(AuditEventRecordCommand command);

    List<AuditEventItem> listByEntity(Long tenantId, String entityType, Long entityId);
}
