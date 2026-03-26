package com.renda.merchantops.domain.audit;

import com.renda.merchantops.domain.shared.error.BizException;
import com.renda.merchantops.domain.shared.error.ErrorCode;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

public class AuditEventDomainService implements AuditEventUseCase {

    private static final String APPROVAL_STATUS_NOT_REQUIRED = "NOT_REQUIRED";

    private final AuditEventPort auditEventPort;

    public AuditEventDomainService(AuditEventPort auditEventPort) {
        this.auditEventPort = auditEventPort;
    }

    @Override
    public void recordEvent(AuditEventRecordCommand command) {
        if (command == null
                || command.tenantId() == null
                || command.entityId() == null
                || command.operatorId() == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "audit event context missing");
        }
        if (!hasText(command.entityType()) || !hasText(command.actionType()) || !hasText(command.requestId())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "audit event required fields missing");
        }
        auditEventPort.save(new NewAuditEvent(
                command.tenantId(),
                normalizeKey(command.entityType()),
                command.entityId(),
                normalizeKey(command.actionType()),
                command.operatorId(),
                command.requestId().trim(),
                normalizeNullable(command.beforeValue()),
                normalizeNullable(command.afterValue()),
                APPROVAL_STATUS_NOT_REQUIRED,
                LocalDateTime.now()
        ));
    }

    @Override
    public List<AuditEventItem> listByEntity(Long tenantId, String entityType, Long entityId) {
        if (tenantId == null || entityId == null || !hasText(entityType)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "entityType and entityId are required");
        }
        return auditEventPort.findAllByEntity(tenantId, normalizeKey(entityType), entityId);
    }

    private String normalizeKey(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeNullable(String value) {
        if (!hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
