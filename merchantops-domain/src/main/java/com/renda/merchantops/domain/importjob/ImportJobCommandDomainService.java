package com.renda.merchantops.domain.importjob;

import com.renda.merchantops.domain.shared.error.BizException;
import com.renda.merchantops.domain.shared.error.ErrorCode;

import java.time.LocalDateTime;
import java.util.Locale;

public class ImportJobCommandDomainService implements ImportJobCommandUseCase {

    private static final String STATUS_QUEUED = "QUEUED";
    private static final String SUPPORTED_IMPORT_TYPE = "USER_CSV";

    private final ImportJobCommandPort importJobCommandPort;

    public ImportJobCommandDomainService(ImportJobCommandPort importJobCommandPort) {
        this.importJobCommandPort = importJobCommandPort;
    }

    @Override
    public ImportJobRecord createQueuedJob(Long tenantId, Long operatorId, String requestId, NewImportJobDraft draft) {
        return createJob(tenantId, operatorId, requestId, draft);
    }

    @Override
    public ImportJobRecord createReplayJob(Long tenantId, Long operatorId, String requestId, NewImportJobDraft draft) {
        // Replay jobs reuse the same queued lifecycle so worker, recovery, and audit flows do
        // not need a parallel execution path just for derived files.
        return createJob(tenantId, operatorId, requestId, draft);
    }

    private ImportJobRecord createJob(Long tenantId, Long operatorId, String requestId, NewImportJobDraft draft) {
        requireOperator(tenantId, operatorId);
        LocalDateTime now = LocalDateTime.now();
        return importJobCommandPort.saveJob(new ImportJobRecord(
                null,
                tenantId,
                normalizeImportType(draft == null ? null : draft.importType()),
                normalizeRequiredValue(draft == null ? null : draft.sourceType(), "sourceType").toUpperCase(Locale.ROOT),
                normalizeRequiredValue(draft == null ? null : draft.sourceFilename(), "sourceFilename"),
                normalizeRequiredValue(draft == null ? null : draft.storageKey(), "storageKey"),
                draft == null ? null : draft.sourceJobId(),
                STATUS_QUEUED,
                operatorId,
                requireRequestId(requestId),
                0,
                0,
                0,
                null,
                now,
                null,
                null
        ));
    }

    private void requireOperator(Long tenantId, Long operatorId) {
        if (tenantId == null || operatorId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "user context missing");
        }
    }

    private String requireRequestId(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "request id missing");
        }
        return requestId.trim();
    }

    private String normalizeImportType(String importType) {
        if (importType == null || importType.isBlank()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "importType must not be blank");
        }
        String normalized = importType.trim().toUpperCase(Locale.ROOT);
        // The public import surface is intentionally narrow for now; keep the domain guard
        // explicit so new import types cannot slip in through API-only validation.
        if (!SUPPORTED_IMPORT_TYPE.equals(normalized)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "importType must be USER_CSV");
        }
        return normalized;
    }

    private String normalizeRequiredValue(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BizException(ErrorCode.BAD_REQUEST, fieldName + " must not be blank");
        }
        return value.trim();
    }
}
