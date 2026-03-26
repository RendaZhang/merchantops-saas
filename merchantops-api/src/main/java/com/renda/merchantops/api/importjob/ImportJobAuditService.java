package com.renda.merchantops.api.importjob;

import com.renda.merchantops.api.audit.AuditEventService;
import com.renda.merchantops.domain.importjob.ImportJobRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
class ImportJobAuditService {

    private final AuditEventService auditEventService;

    void recordReplayRequestedEvent(Long tenantId,
                                    Long operatorId,
                                    String requestId,
                                    Long sourceJobId,
                                    Long replayJobId,
                                    int replayRowCount,
                                    Map<String, Object> replayMetadata) {
        Map<String, Object> afterValue = new LinkedHashMap<>();
        afterValue.put("replayJobId", replayJobId);
        afterValue.put("replayedFailureCount", replayRowCount);
        if (replayMetadata != null && !replayMetadata.isEmpty()) {
            afterValue.putAll(replayMetadata);
        }
        auditEventService.recordEvent(
                tenantId,
                "IMPORT_JOB",
                sourceJobId,
                "IMPORT_JOB_REPLAY_REQUESTED",
                operatorId,
                requestId,
                null,
                afterValue
        );
    }

    void recordImportJobCreatedEvent(ImportJobRecord job,
                                     Long operatorId,
                                     String requestId,
                                     Map<String, Object> replayMetadata) {
        Map<String, Object> afterValue = new LinkedHashMap<>();
        afterValue.put("status", job.status());
        afterValue.put("importType", job.importType());
        afterValue.put("sourceFilename", job.sourceFilename());
        if (job.sourceJobId() != null) {
            afterValue.put("sourceJobId", job.sourceJobId());
        }
        if (replayMetadata != null && !replayMetadata.isEmpty()) {
            afterValue.putAll(replayMetadata);
        }
        auditEventService.recordEvent(
                job.tenantId(),
                "IMPORT_JOB",
                job.id(),
                "IMPORT_JOB_CREATED",
                operatorId,
                requestId,
                null,
                afterValue
        );
    }
}
