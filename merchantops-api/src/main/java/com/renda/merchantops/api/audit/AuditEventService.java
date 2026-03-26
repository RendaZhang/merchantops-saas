package com.renda.merchantops.api.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.renda.merchantops.api.context.RequestIdPolicy;
import com.renda.merchantops.api.dto.audit.query.AuditEventListResponse;
import com.renda.merchantops.api.dto.audit.query.AuditEventResponse;
import com.renda.merchantops.domain.audit.AuditEventRecordCommand;
import com.renda.merchantops.domain.audit.AuditEventUseCase;
import com.renda.merchantops.domain.shared.error.BizException;
import com.renda.merchantops.domain.shared.error.ErrorCode;
import com.renda.merchantops.domain.ticket.TicketAuditPort;
import com.renda.merchantops.domain.user.UserAuditPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditEventService implements UserAuditPort, TicketAuditPort {

    private final AuditEventUseCase auditEventUseCase;
    private final ObjectMapper objectMapper;

    @Override
    public void recordEvent(Long tenantId,
                            String entityType,
                            Long entityId,
                            String actionType,
                            Long operatorId,
                            String requestId,
                            Object beforeValue,
                            Object afterValue) {
        auditEventUseCase.recordEvent(new AuditEventRecordCommand(
                tenantId,
                entityType,
                entityId,
                actionType,
                operatorId,
                RequestIdPolicy.requireNormalized(requestId),
                toJson(beforeValue),
                toJson(afterValue)
        ));
    }

    public AuditEventListResponse listByEntity(Long tenantId, String entityType, Long entityId) {
        return new AuditEventListResponse(
                auditEventUseCase.listByEntity(tenantId, entityType, entityId)
                        .stream()
                        .map(event -> new AuditEventResponse(
                                event.id(),
                                event.entityType(),
                                event.entityId(),
                                event.actionType(),
                                event.operatorId(),
                                event.requestId(),
                                event.beforeValue(),
                                event.afterValue(),
                                event.approvalStatus(),
                                event.createdAt()
                        ))
                        .toList()
        );
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BizException(ErrorCode.BIZ_ERROR, "failed to serialize audit snapshot");
        }
    }
}
