package com.renda.merchantops.domain.audit;

import com.renda.merchantops.domain.shared.error.BizException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuditEventDomainServiceTest {

    @Test
    void recordEventShouldNormalizeKeysAndPersistSerializedValues() {
        CapturingAuditEventPort port = new CapturingAuditEventPort();
        AuditEventUseCase useCase = new AuditEventDomainService(port);

        useCase.recordEvent(new AuditEventRecordCommand(
                1L,
                " import_job ",
                9L,
                " import_job_created ",
                101L,
                "req-1",
                "{\"before\":1}",
                "{\"after\":2}"
        ));

        assertThat(port.savedEvent.entityType()).isEqualTo("IMPORT_JOB");
        assertThat(port.savedEvent.actionType()).isEqualTo("IMPORT_JOB_CREATED");
        assertThat(port.savedEvent.beforeValue()).isEqualTo("{\"before\":1}");
        assertThat(port.savedEvent.afterValue()).isEqualTo("{\"after\":2}");
        assertThat(port.savedEvent.approvalStatus()).isEqualTo("NOT_REQUIRED");
        assertThat(port.savedEvent.createdAt()).isNotNull();
    }

    @Test
    void listByEntityShouldRejectBlankEntityType() {
        AuditEventUseCase useCase = new AuditEventDomainService(new CapturingAuditEventPort());

        assertThatThrownBy(() -> useCase.listByEntity(1L, " ", 9L))
                .isInstanceOf(BizException.class)
                .hasMessage("entityType and entityId are required");
    }

    private static final class CapturingAuditEventPort implements AuditEventPort {

        private NewAuditEvent savedEvent;

        @Override
        public void save(NewAuditEvent event) {
            this.savedEvent = event;
        }

        @Override
        public List<AuditEventItem> findAllByEntity(Long tenantId, String entityType, Long entityId) {
            return List.of();
        }
    }
}
