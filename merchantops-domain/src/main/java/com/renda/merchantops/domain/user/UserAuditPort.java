package com.renda.merchantops.domain.user;

public interface UserAuditPort {

    void recordEvent(Long tenantId,
                     String entityType,
                     Long entityId,
                     String actionType,
                     Long operatorId,
                     String requestId,
                     Object beforeValue,
                     Object afterValue);
}
