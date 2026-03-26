package com.renda.merchantops.domain.ticket;

public interface TicketAuditPort {

    void recordEvent(Long tenantId,
                     String entityType,
                     Long entityId,
                     String actionType,
                     Long operatorId,
                     String requestId,
                     Object beforeValue,
                     Object afterValue);
}
