package com.renda.merchantops.infra.audit;

import com.renda.merchantops.domain.audit.NewAuditEvent;
import com.renda.merchantops.infra.persistence.entity.AuditEventEntity;
import com.renda.merchantops.infra.repository.AuditEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaAuditEventAdapterTest {

    @Mock
    private AuditEventRepository auditEventRepository;

    @Test
    void saveShouldPersistDomainAuditEventFields() {
        JpaAuditEventAdapter adapter = new JpaAuditEventAdapter(auditEventRepository);

        adapter.save(new NewAuditEvent(
                1L,
                "IMPORT_JOB",
                9L,
                "IMPORT_JOB_CREATED",
                101L,
                "req-1",
                null,
                "{\"status\":\"QUEUED\"}",
                "NOT_REQUIRED",
                LocalDateTime.of(2026, 3, 26, 10, 0)
        ));

        ArgumentCaptor<AuditEventEntity> entityCaptor = ArgumentCaptor.forClass(AuditEventEntity.class);
        verify(auditEventRepository).save(entityCaptor.capture());
        assertThat(entityCaptor.getValue().getEntityType()).isEqualTo("IMPORT_JOB");
        assertThat(entityCaptor.getValue().getActionType()).isEqualTo("IMPORT_JOB_CREATED");
        assertThat(entityCaptor.getValue().getOperatorId()).isEqualTo(101L);
    }

    @Test
    void findAllByEntityShouldMapRepositoryItemsIntoDomainItems() {
        JpaAuditEventAdapter adapter = new JpaAuditEventAdapter(auditEventRepository);
        AuditEventEntity entity = new AuditEventEntity();
        entity.setId(8L);
        entity.setEntityType("IMPORT_JOB");
        entity.setEntityId(9L);
        entity.setActionType("IMPORT_JOB_CREATED");
        entity.setOperatorId(101L);
        entity.setRequestId("req-1");
        entity.setAfterValue("{\"status\":\"QUEUED\"}");
        entity.setApprovalStatus("NOT_REQUIRED");
        entity.setCreatedAt(LocalDateTime.of(2026, 3, 26, 10, 0));
        when(auditEventRepository.findAllByTenantIdAndEntityTypeAndEntityIdOrderByIdAsc(1L, "IMPORT_JOB", 9L))
                .thenReturn(List.of(entity));

        var result = adapter.findAllByEntity(1L, "IMPORT_JOB", 9L);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().actionType()).isEqualTo("IMPORT_JOB_CREATED");
        assertThat(result.getFirst().requestId()).isEqualTo("req-1");
    }
}
