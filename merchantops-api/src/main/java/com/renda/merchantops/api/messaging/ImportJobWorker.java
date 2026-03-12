package com.renda.merchantops.api.messaging;

import com.renda.merchantops.api.config.ImportJobMessagingConfig;
import com.renda.merchantops.api.service.AuditEventService;
import com.renda.merchantops.api.service.ImportCsvSupport;
import com.renda.merchantops.api.service.ImportFileStorageService;
import com.renda.merchantops.infra.persistence.entity.ImportJobEntity;
import com.renda.merchantops.infra.persistence.entity.ImportJobItemErrorEntity;
import com.renda.merchantops.infra.repository.ImportJobItemErrorRepository;
import com.renda.merchantops.infra.repository.ImportJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

@Component
@RequiredArgsConstructor
@Slf4j
public class ImportJobWorker {

    private static final String UTF8_BOM = "\uFEFF";
    private final ImportJobRepository importJobRepository;
    private final ImportJobItemErrorRepository importJobItemErrorRepository;
    private final ImportFileStorageService importFileStorageService;
    private final AuditEventService auditEventService;
    private final UserCsvImportProcessor userCsvImportProcessor;

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
                    new InputStreamReader(importFileStorageService.openStream(job.getStorageKey()), StandardCharsets.UTF_8));
                 CSVParser parser = ImportCsvSupport.IMPORT_CSV_FORMAT.parse(reader)) {
                var records = parser.iterator();
                if (!records.hasNext()) {
                    saveError(job, null, "EMPTY_FILE", "csv file is empty", null);
                    summary = "csv file is empty";
                    failure = 1;
                } else {
                    CSVRecord headerRecord = records.next();
                    String headerRawPayload = toRawPayload(headerRecord);
                    if (!"USER_CSV".equals(job.getImportType())) {
                        saveError(job, 0, "UNSUPPORTED_IMPORT_TYPE", "unsupported import type: " + job.getImportType(), headerRawPayload);
                        summary = "unsupported import type";
                        failure = 1;
                    } else {
                        List<String> headerColumns = normalizeHeaderColumns(headerRecord);
                        if (!ImportCsvSupport.USER_CSV_HEADERS.equals(headerColumns)) {
                            saveError(job, 0, "INVALID_HEADER", "header must be: " + String.join(",", ImportCsvSupport.USER_CSV_HEADERS), headerRawPayload);
                            summary = "invalid header";
                            failure++;
                        } else {
                            int rowNumber = 1;
                            while (records.hasNext()) {
                                CSVRecord record = records.next();
                                rowNumber++;
                                total++;
                                List<String> columns = parseColumns(record);
                                String rawPayload = toRawPayload(record);
                                if (columns.size() != ImportCsvSupport.USER_CSV_HEADERS.size()) {
                                    failure++;
                                    saveError(job, rowNumber, "INVALID_ROW_SHAPE", "column count mismatch", rawPayload);
                                    continue;
                                }
                                try {
                                    userCsvImportProcessor.processRow(job, rowNumber, columns);
                                    success++;
                                } catch (ImportRowProcessingException ex) {
                                    failure++;
                                    saveError(job, rowNumber, ex.code(), ex.getMessage(), rawPayload);
                                }
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
                    job.setErrorSummary("completed with some row errors");
                }
                auditEventService.recordEvent(job.getTenantId(), "IMPORT_JOB", job.getId(), "IMPORT_JOB_COMPLETED",
                        job.getRequestedBy(), job.getRequestId(), null,
                        Map.of("status", "SUCCEEDED", "successCount", success, "failureCount", failure, "totalCount", total));
            }
            importJobRepository.save(job);
        } catch (IOException | UncheckedIOException ex) {
            Throwable cause = ex instanceof UncheckedIOException uncheckedIOException ? uncheckedIOException.getCause() : ex;
            log.error("failed to process import job {}", job.getId(), ex);
            saveError(job, null, "FILE_READ_ERROR", "failed to read import file", cause == null ? ex.getMessage() : cause.getMessage());
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

    private List<String> parseColumns(CSVRecord row) {
        return StreamSupport.stream(row.spliterator(), false)
                .toList();
    }

    private List<String> normalizeHeaderColumns(CSVRecord row) {
        List<String> columns = parseColumns(row);
        if (columns.isEmpty()) {
            return columns;
        }
        return java.util.stream.IntStream.range(0, columns.size())
                .mapToObj(index -> normalizeHeaderValue(columns.get(index), index == 0))
                .toList();
    }

    private String normalizeHeaderValue(String value, boolean stripBom) {
        String normalized = value == null ? "" : value;
        if (stripBom && normalized.startsWith(UTF8_BOM)) {
            normalized = normalized.substring(1);
        }
        return normalized.trim();
    }

    private String toRawPayload(CSVRecord row) {
        try (StringWriter writer = new StringWriter();
             CSVPrinter printer = new CSVPrinter(writer, ImportCsvSupport.RAW_PAYLOAD_CSV_FORMAT)) {
            printer.printRecord(row);
            String rawPayload = writer.toString();
            return rawPayload.endsWith("\n") ? rawPayload.substring(0, rawPayload.length() - 1) : rawPayload;
        } catch (IOException ex) {
            throw new UncheckedIOException("failed to serialize csv row", ex);
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
