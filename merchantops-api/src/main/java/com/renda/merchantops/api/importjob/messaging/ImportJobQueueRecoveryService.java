package com.renda.merchantops.api.importjob.messaging;

import com.renda.merchantops.api.config.ImportProcessingProperties;
import com.renda.merchantops.domain.importjob.ImportJobQueryUseCase;
import com.renda.merchantops.domain.importjob.ImportJobRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class ImportJobQueueRecoveryService {

    private final ImportJobQueryUseCase importJobQueryUseCase;
    private final ImportJobPublisher importJobPublisher;
    private final ImportProcessingProperties importProcessingProperties;

    @Scheduled(
            fixedDelayString = "${merchantops.import.processing.enqueue-recovery-delay-ms:300000}",
            initialDelayString = "${merchantops.import.processing.enqueue-recovery-delay-ms:300000}"
    )
    public void recoverQueuedJobs() {
        republishStaleQueuedJobs();
    }

    public int republishStaleQueuedJobs() {
        LocalDateTime createdBefore = LocalDateTime.now()
                .minusSeconds(importProcessingProperties.getEnqueueRecoveryMinAgeSeconds());
        var queuedJobs = importJobQueryUseCase.listQueuedJobsForRecovery(
                createdBefore,
                importProcessingProperties.getEnqueueRecoveryBatchSize()
        );
        int publishedCount = 0;
        for (ImportJobRecord job : queuedJobs) {
            try {
                importJobPublisher.publish(new ImportJobMessage(job.id(), job.tenantId()));
                publishedCount++;
            } catch (RuntimeException ex) {
                log.warn("failed to republish queued import job {}", job.id(), ex);
            }
        }
        return publishedCount;
    }
}
