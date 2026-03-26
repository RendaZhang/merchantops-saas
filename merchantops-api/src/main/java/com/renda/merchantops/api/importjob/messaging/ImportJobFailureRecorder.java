package com.renda.merchantops.api.importjob.messaging;

import com.renda.merchantops.api.audit.AuditEventService;
import com.renda.merchantops.domain.importjob.ImportJobCommandPort;
import com.renda.merchantops.domain.importjob.ImportJobErrorRecord;
import com.renda.merchantops.domain.importjob.ImportJobRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
class ImportJobFailureRecorder {

    private final ImportJobCommandPort importJobCommandPort;
    private final AuditEventService auditEventService;

    void saveRowError(ImportJobRecord job, Integer rowNumber, String code, String message, String rawPayload) {
        importJobCommandPort.saveJobError(new ImportJobErrorRecord(
                null,
                job.tenantId(),
                job.id(),
                rowNumber,
                code,
                message,
                rawPayload,
                LocalDateTime.now()
        ));
    }

    void recordJobFailure(ImportJobRecord saved, String errorCode) {
        Map<String, Object> afterValue = new LinkedHashMap<>();
        afterValue.put("status", "FAILED");
        if (errorCode != null) {
            afterValue.put("error", errorCode);
        }
        afterValue.put("successCount", safeInt(saved.successCount()));
        afterValue.put("failureCount", safeInt(saved.failureCount()));
        afterValue.put("totalCount", safeInt(saved.totalCount()));

        auditEventService.recordEvent(
                saved.tenantId(),
                "IMPORT_JOB",
                saved.id(),
                "IMPORT_JOB_FAILED",
                saved.requestedBy(),
                saved.requestId(),
                null,
                afterValue
        );
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }
}
