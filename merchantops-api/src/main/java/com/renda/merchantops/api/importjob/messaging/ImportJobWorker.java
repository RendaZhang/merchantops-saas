package com.renda.merchantops.api.importjob.messaging;

import com.renda.merchantops.api.config.ImportJobMessagingConfig;
import com.renda.merchantops.api.config.ImportProcessingProperties;
import com.renda.merchantops.api.importjob.ImportCsvSupport;
import com.renda.merchantops.api.importjob.ImportFileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

@Component
@RequiredArgsConstructor
@Slf4j
public class ImportJobWorker {

    private static final String UTF8_BOM = "\uFEFF";
    private static final String IMPORT_TYPE_USER_CSV = "USER_CSV";

    private final ImportFileStorageService importFileStorageService;
    private final ImportJobExecutionCoordinator importJobExecutionCoordinator;
    private final ImportProcessingProperties importProcessingProperties;

    @RabbitListener(queues = ImportJobMessagingConfig.IMPORT_JOB_QUEUE)
    public void consume(ImportJobMessage message) {
        if (message == null || message.jobId() == null || message.tenantId() == null) {
            return;
        }
        ImportJobStartResult startResult =
                importJobExecutionCoordinator.startProcessing(message.jobId(), message.tenantId());
        if (startResult.action() == ImportJobStartAction.REQUEUE) {
            log.debug("acknowledged duplicate import job message for job {} tenant {} while job is still processing",
                    message.jobId(), message.tenantId());
            return;
        }
        ImportJobExecutionContext context = startResult.context();
        if (context == null) {
            log.debug("ignored import job message for job {} tenant {}", message.jobId(), message.tenantId());
            return;
        }

        List<ImportJobChunkRow> chunk =
                new ArrayList<>(importProcessingProperties.getChunkSize());
        int flushedDataRows = 0;
        int rowNumber = 1;

        try {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(importFileStorageService.openStream(context.storageKey()), StandardCharsets.UTF_8));
                 CSVParser parser = ImportCsvSupport.IMPORT_CSV_FORMAT.parse(reader)) {
                var records = parser.iterator();
                if (!records.hasNext()) {
                    importJobExecutionCoordinator.failJob(context, new ImportJobFailure(
                            null,
                            "EMPTY_FILE",
                            "csv file is empty",
                            null,
                            "csv file is empty",
                            0,
                            1
                    ));
                    return;
                }

                CSVRecord headerRecord = records.next();
                String headerRawPayload = toRawPayload(headerRecord);
                if (!IMPORT_TYPE_USER_CSV.equals(context.importType())) {
                    importJobExecutionCoordinator.failJob(context, new ImportJobFailure(
                            null,
                            "UNSUPPORTED_IMPORT_TYPE",
                            "unsupported import type: " + context.importType(),
                            headerRawPayload,
                            "unsupported import type",
                            0,
                            1
                    ));
                    return;
                }

                List<String> headerColumns = normalizeHeaderColumns(headerRecord);
                if (!ImportCsvSupport.USER_CSV_HEADERS.equals(headerColumns)) {
                    importJobExecutionCoordinator.failJob(context, new ImportJobFailure(
                            null,
                            "INVALID_HEADER",
                            "header must be: " + String.join(",", ImportCsvSupport.USER_CSV_HEADERS),
                            headerRawPayload,
                            "invalid header",
                            0,
                            1
                    ));
                    return;
                }

                while (records.hasNext()) {
                    CSVRecord record = records.next();
                    rowNumber++;
                    int nextDataRowCount = flushedDataRows + chunk.size() + 1;
                    if (nextDataRowCount > importProcessingProperties.getMaxRowsPerJob()) {
                        if (!flushChunk(context, chunk)) {
                            return;
                        }
                        importJobExecutionCoordinator.failJob(context, new ImportJobFailure(
                                null,
                                "MAX_ROWS_EXCEEDED",
                                "data row limit exceeded at row " + rowNumber
                                        + "; max allowed is " + importProcessingProperties.getMaxRowsPerJob(),
                                toRawPayload(record),
                                "import job exceeded max row limit",
                                1,
                                1
                        ));
                        return;
                    }
                    chunk.add(new ImportJobChunkRow(
                            rowNumber,
                            parseColumns(record),
                            toRawPayload(record)
                    ));
                    if (chunk.size() >= importProcessingProperties.getChunkSize()) {
                        flushedDataRows += chunk.size();
                        if (!flushChunk(context, chunk)) {
                            return;
                        }
                    }
                }
            }

            if (!flushChunk(context, chunk)) {
                return;
            }
            importJobExecutionCoordinator.completeJob(context);
        } catch (IOException | UncheckedIOException ex) {
            flushChunkQuietly(context, chunk);
            Throwable cause = ex instanceof UncheckedIOException uncheckedIOException ? uncheckedIOException.getCause() : ex;
            log.error("failed to process import job {}", context.jobId(), ex);
            importJobExecutionCoordinator.failJob(context, new ImportJobFailure(
                    null,
                    "FILE_READ_ERROR",
                    "failed to read import file",
                    cause == null ? ex.getMessage() : cause.getMessage(),
                    "failed to read import file",
                    0,
                    1
            ));
        } catch (RuntimeException ex) {
            log.error("failed to execute import job {}", context.jobId(), ex);
            importJobExecutionCoordinator.failJob(context, new ImportJobFailure(
                    null,
                    "PROCESSING_ERROR",
                    "import job processing failed",
                    ex.getMessage(),
                    "import job processing failed",
                    0,
                    1
            ));
        }
    }

    private boolean flushChunk(ImportJobExecutionContext context,
                               List<ImportJobChunkRow> chunk) {
        if (chunk.isEmpty()) {
            return true;
        }
        boolean processed = importJobExecutionCoordinator.processChunk(context, List.copyOf(chunk));
        chunk.clear();
        if (!processed) {
            log.debug("stopped import job {} after chunk because execution is no longer active", context.jobId());
        }
        return processed;
    }

    private void flushChunkQuietly(ImportJobExecutionContext context,
                                   List<ImportJobChunkRow> chunk) {
        if (chunk.isEmpty()) {
            return;
        }
        try {
            flushChunk(context, chunk);
        } catch (RuntimeException flushEx) {
            log.error("failed to flush pending chunk for import job {}", context.jobId(), flushEx);
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
        return IntStream.range(0, columns.size())
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
}
