package com.renda.merchantops.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.renda.merchantops.api.context.RequestIdPolicy;
import com.renda.merchantops.api.dto.audit.query.AuditEventListResponse;
import com.renda.merchantops.api.dto.audit.query.AuditEventResponse;
import com.renda.merchantops.common.exception.BizException;
import com.renda.merchantops.common.exception.ErrorCode;
import com.renda.merchantops.infra.persistence.entity.AuditEventEntity;
import com.renda.merchantops.infra.repository.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuditEventService {

    private static final String APPROVAL_STATUS_NOT_REQUIRED = "NOT_REQUIRED";

    private final AuditEventRepository auditEventRepository;
    private final ObjectMapper objectMapper;

    public void recordEvent(Long tenantId,
                            String entityType,
                            Long entityId,
                            String actionType,
                            Long operatorId,
                            String requestId,
                            Object beforeValue,
                            Object afterValue) {
        String resolvedRequestId = RequestIdPolicy.requireNormalized(requestId);
        if (tenantId == null || entityId == null || operatorId == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "audit event context missing");
        }
        if (!StringUtils.hasText(entityType) || !StringUtils.hasText(actionType)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "audit event required fields missing");
        }

        AuditEventEntity event = new AuditEventEntity();
        event.setTenantId(tenantId);
        event.setEntityType(normalizeKey(entityType));
        event.setEntityId(entityId);
        event.setActionType(normalizeKey(actionType));
        event.setOperatorId(operatorId);
        event.setRequestId(resolvedRequestId);
        event.setBeforeValue(toJson(beforeValue));
        event.setAfterValue(toJson(afterValue));
        event.setApprovalStatus(APPROVAL_STATUS_NOT_REQUIRED);
        event.setCreatedAt(LocalDateTime.now());
        auditEventRepository.save(event);
    }

    public AuditEventListResponse listByEntity(Long tenantId, String entityType, Long entityId) {
        if (tenantId == null || entityId == null || !StringUtils.hasText(entityType)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "entityType and entityId are required");
        }
        String normalizedEntityType = normalizeKey(entityType);
        return new AuditEventListResponse(
                auditEventRepository.findAllByTenantIdAndEntityTypeAndEntityIdOrderByIdAsc(tenantId, normalizedEntityType, entityId)
                        .stream()
                        .map(event -> new AuditEventResponse(
                                event.getId(),
                                event.getEntityType(),
                                event.getEntityId(),
                                event.getActionType(),
                                event.getOperatorId(),
                                event.getRequestId(),
                                event.getBeforeValue(),
                                event.getAfterValue(),
                                event.getApprovalStatus(),
                                event.getCreatedAt()
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

    private String normalizeKey(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
