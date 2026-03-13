package com.renda.merchantops.api.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ImportJobCreatedEventListener {

    private final ImportJobPublisher importJobPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ImportJobCreatedEvent event) {
        if (event == null || event.jobId() == null || event.tenantId() == null) {
            return;
        }
        importJobPublisher.publish(new ImportJobMessage(event.jobId(), event.tenantId()));
    }
}
