package com.renda.merchantops.api.importjob.messaging;

import com.renda.merchantops.api.importjob.ImportCsvSupport;
import com.renda.merchantops.domain.importjob.ImportJobCommandPort;
import com.renda.merchantops.domain.importjob.ImportJobRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
class ImportJobChunkProcessor {

    private static final String IMPORT_TYPE_USER_CSV = "USER_CSV";

    private final ImportJobCommandPort importJobCommandPort;
    private final UserCsvImportProcessor userCsvImportProcessor;
    private final ImportJobFailureRecorder importJobFailureRecorder;

    ImportJobRecord processChunk(ImportJobRecord job, ImportJobExecutionContext context, List<ImportJobChunkRow> rows) {
        if (!IMPORT_TYPE_USER_CSV.equals(job.importType())) {
            throw new IllegalStateException("unsupported import type for chunk processing: " + job.importType());
        }

        int totalDelta = 0;
        int successDelta = 0;
        int failureDelta = 0;
        RuntimeException unexpectedFailure = null;
        for (ImportJobChunkRow row : rows) {
            totalDelta++;
            if (row.columns().size() != ImportCsvSupport.USER_CSV_HEADERS.size()) {
                failureDelta++;
                importJobFailureRecorder.saveRowError(job, row.rowNumber(), "INVALID_ROW_SHAPE", "column count mismatch", row.rawPayload());
                continue;
            }
            try {
                userCsvImportProcessor.processRow(context, row.rowNumber(), row.columns());
                successDelta++;
            } catch (ImportRowProcessingException ex) {
                failureDelta++;
                importJobFailureRecorder.saveRowError(job, row.rowNumber(), ex.code(), ex.getMessage(), row.rawPayload());
            } catch (RuntimeException ex) {
                unexpectedFailure = ex;
                break;
            }
        }

        ImportJobRecord saved = importJobCommandPort.saveJob(new ImportJobRecord(
                job.id(),
                job.tenantId(),
                job.importType(),
                job.sourceType(),
                job.sourceFilename(),
                job.storageKey(),
                job.sourceJobId(),
                job.status(),
                job.requestedBy(),
                job.requestId(),
                safeInt(job.totalCount()) + totalDelta,
                safeInt(job.successCount()) + successDelta,
                safeInt(job.failureCount()) + failureDelta,
                job.errorSummary(),
                job.createdAt(),
                job.startedAt(),
                job.finishedAt()
        ));
        if (unexpectedFailure != null) {
            throw unexpectedFailure;
        }
        return saved;
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }
}
