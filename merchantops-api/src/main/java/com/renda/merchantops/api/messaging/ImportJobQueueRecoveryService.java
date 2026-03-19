package com.renda.merchantops.api.messaging;

import com.renda.merchantops.api.config.ImportProcessingProperties;
import com.renda.merchantops.infra.persistence.entity.ImportJobEntity;
import com.renda.merchantops.infra.repository.ImportJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ImportJobQueueRecoveryService {

    private static final String STATUS_QUEUED = "QUEUED";

    private final ImportJobRepository importJobRepository;
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
        List<ImportJobEntity> queuedJobs = importJobRepository.findQueuedJobsForEnqueueRecovery(
                STATUS_QUEUED,
                createdBefore,
                PageRequest.of(0, importProcessingProperties.getEnqueueRecoveryBatchSize())
        );
        int publishedCount = 0;
        for (ImportJobEntity job : queuedJobs) {
            try {
                importJobPublisher.publish(new ImportJobMessage(job.getId(), job.getTenantId()));
                publishedCount++;
            } catch (RuntimeException ex) {
                log.warn("failed to republish queued import job {}", job.getId(), ex);
            }
        }
        return publishedCount;
    }
}
