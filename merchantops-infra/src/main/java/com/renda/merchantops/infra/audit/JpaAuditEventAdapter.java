package com.renda.merchantops.infra.audit;

import com.renda.merchantops.domain.audit.AuditEventItem;
import com.renda.merchantops.domain.audit.AuditEventPort;
import com.renda.merchantops.domain.audit.NewAuditEvent;
import com.renda.merchantops.infra.persistence.entity.AuditEventEntity;
import com.renda.merchantops.infra.repository.AuditEventRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JpaAuditEventAdapter implements AuditEventPort {

    private final AuditEventRepository auditEventRepository;

    public JpaAuditEventAdapter(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @Override
    public void save(NewAuditEvent event) {
        AuditEventEntity entity = new AuditEventEntity();
        entity.setTenantId(event.tenantId());
        entity.setEntityType(event.entityType());
        entity.setEntityId(event.entityId());
        entity.setActionType(event.actionType());
        entity.setOperatorId(event.operatorId());
        entity.setRequestId(event.requestId());
        entity.setBeforeValue(event.beforeValue());
        entity.setAfterValue(event.afterValue());
        entity.setApprovalStatus(event.approvalStatus());
        entity.setCreatedAt(event.createdAt());
        auditEventRepository.save(entity);
    }

    @Override
    public List<AuditEventItem> findAllByEntity(Long tenantId, String entityType, Long entityId) {
        return auditEventRepository.findAllByTenantIdAndEntityTypeAndEntityIdOrderByIdAsc(tenantId, entityType, entityId)
                .stream()
                .map(event -> new AuditEventItem(
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
                .toList();
    }
}
