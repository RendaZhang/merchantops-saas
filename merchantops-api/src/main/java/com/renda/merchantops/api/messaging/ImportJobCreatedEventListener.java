package com.renda.merchantops.api.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class ImportJobCreatedEventListener {

    private final ImportJobPublisher importJobPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ImportJobCreatedEvent event) {
        if (event == null || event.jobId() == null || event.tenantId() == null) {
            return;
        }
        try {
            importJobPublisher.publish(new ImportJobMessage(event.jobId(), event.tenantId()));
        } catch (RuntimeException ex) {
            // QUEUED status persists enqueue intent; queued-job recovery will retry publish.
            log.warn("failed to publish import job {} after commit; queued-job recovery will retry", event.jobId(), ex);
        }
    }
}
