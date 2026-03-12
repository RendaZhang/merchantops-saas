package com.renda.merchantops.api.messaging;

import com.renda.merchantops.api.config.ImportJobMessagingConfig;
import com.renda.merchantops.api.service.AuditEventService;
import com.renda.merchantops.api.service.ImportFileStorageService;
import com.renda.merchantops.infra.persistence.entity.ImportJobEntity;
import com.renda.merchantops.infra.persistence.entity.ImportJobItemErrorEntity;
import com.renda.merchantops.infra.repository.ImportJobItemErrorRepository;
import com.renda.merchantops.infra.repository.ImportJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ImportJobWorker {

    private final ImportJobRepository importJobRepository;
    private final ImportJobItemErrorRepository importJobItemErrorRepository;
    private final ImportFileStorageService importFileStorageService;
    private final AuditEventService auditEventService;

    @RabbitListener(queues = ImportJobMessagingConfig.IMPORT_JOB_QUEUE)
    @Transactional
    public void consume(ImportJobMessage message) {
        if (message == null || message.jobId() == null || message.tenantId() == null) {
            return;
        }
        ImportJobEntity job = importJobRepository.findByIdAndTenantIdForUpdate(message.jobId(), message.tenantId()).orElse(null);
        if (job == null || !"QUEUED".equals(job.getStatus())) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        job.setStatus("PROCESSING");
        job.setStartedAt(now);
        importJobRepository.save(job);
        auditEventService.recordEvent(job.getTenantId(), "IMPORT_JOB", job.getId(), "IMPORT_JOB_PROCESSING_STARTED",
                job.getRequestedBy(), job.getRequestId(), null, Map.of("status", "PROCESSING"));

        int total = 0;
        int success = 0;
        int failure = 0;
        String summary = null;

        try {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(importFileStorageService.openStream(job.getStorageKey()), StandardCharsets.UTF_8))) {
                String header = reader.readLine();
                if (header == null) {
                    saveError(job, null, "EMPTY_FILE", "csv file is empty", null);
                    summary = "csv file is empty";
                    failure = 1;
                } else {
                    String[] headerColumns = header.split(",", -1);
                    if (headerColumns.length < 2) {
                        saveError(job, 0, "INVALID_HEADER", "header requires at least 2 columns", header);
                        summary = "invalid header";
                        failure++;
                    } else {
                        String row;
                        int rowNumber = 1;
                        while ((row = reader.readLine()) != null) {
                            rowNumber++;
                            total++;
                            String[] cols = row.split(",", -1);
                            if (cols.length != headerColumns.length) {
                                failure++;
                                saveError(job, rowNumber, "INVALID_ROW_SHAPE", "column count mismatch", row);
                            } else {
                                success++;
                            }
                        }
                    }
                }
            }

            job.setTotalCount(total);
            job.setSuccessCount(success);
            job.setFailureCount(failure);
            job.setFinishedAt(LocalDateTime.now());
            if (failure > 0 && success == 0) {
                job.setStatus("FAILED");
                job.setErrorSummary(summary == null ? "all rows failed validation" : summary);
                auditEventService.recordEvent(job.getTenantId(), "IMPORT_JOB", job.getId(), "IMPORT_JOB_FAILED",
                        job.getRequestedBy(), job.getRequestId(), null,
                        Map.of("status", "FAILED", "failureCount", failure, "totalCount", total));
            } else {
                job.setStatus("SUCCEEDED");
                if (failure > 0) {
                    job.setErrorSummary("completed with some row validation errors");
                }
                auditEventService.recordEvent(job.getTenantId(), "IMPORT_JOB", job.getId(), "IMPORT_JOB_COMPLETED",
                        job.getRequestedBy(), job.getRequestId(), null,
                        Map.of("status", "SUCCEEDED", "successCount", success, "failureCount", failure, "totalCount", total));
            }
            importJobRepository.save(job);
        } catch (IOException ex) {
            log.error("failed to process import job {}", job.getId(), ex);
            saveError(job, null, "FILE_READ_ERROR", "failed to read import file", ex.getMessage());
            job.setStatus("FAILED");
            job.setFinishedAt(LocalDateTime.now());
            job.setErrorSummary("failed to read import file");
            job.setFailureCount(Math.max(failure, 1));
            importJobRepository.save(job);
            auditEventService.recordEvent(job.getTenantId(), "IMPORT_JOB", job.getId(), "IMPORT_JOB_FAILED",
                    job.getRequestedBy(), job.getRequestId(), null,
                    Map.of("status", "FAILED", "error", "FILE_READ_ERROR"));
        }
    }

    private void saveError(ImportJobEntity job, Integer rowNumber, String code, String message, String rawPayload) {
        ImportJobItemErrorEntity error = new ImportJobItemErrorEntity();
        error.setTenantId(job.getTenantId());
        error.setImportJobId(job.getId());
        error.setRowNumber(rowNumber);
        error.setErrorCode(code);
        error.setErrorMessage(message);
        error.setRawPayload(rawPayload);
        error.setCreatedAt(LocalDateTime.now());
        importJobItemErrorRepository.save(error);
    }
}
