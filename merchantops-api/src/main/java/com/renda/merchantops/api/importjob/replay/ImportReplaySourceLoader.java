package com.renda.merchantops.api.importjob.replay;

import com.renda.merchantops.domain.importjob.ImportJobDetail;
import com.renda.merchantops.domain.importjob.ImportJobErrorRecord;
import com.renda.merchantops.domain.importjob.ImportJobQueryUseCase;
import com.renda.merchantops.domain.importjob.ImportJobRecord;
import com.renda.merchantops.domain.shared.error.BizException;
import com.renda.merchantops.domain.shared.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ImportReplaySourceLoader {

    private final ImportJobQueryUseCase importJobQueryUseCase;

    public ReplayableFailedRows loadFailedRowReplay(Long tenantId, Long sourceJobId) {
        ImportJobDetail sourceJobDetail = requireReplayableSourceJob(tenantId, sourceJobId);
        List<ImportJobErrorRecord> failedRows = sourceJobDetail.itemErrors().stream()
                .filter(this::isReplayableRow)
                .toList();
        if (failedRows.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "import job has no replayable failed rows");
        }
        return new ReplayableFailedRows(sourceJobDetail.job(), failedRows);
    }

    public WholeFileReplaySource loadWholeFileReplay(Long tenantId, Long sourceJobId) {
        ImportJobDetail sourceJobDetail = requireWholeFileReplayableSourceJob(tenantId, sourceJobId);
        return new WholeFileReplaySource(sourceJobDetail.job(), resolveWholeFileReplayRowCount(sourceJobDetail.job()));
    }

    public ReplayableFailedRows loadSelectiveFailedRowReplay(Long tenantId, Long sourceJobId, List<String> errorCodes) {
        ImportJobDetail sourceJobDetail = requireReplayableSourceJob(tenantId, sourceJobId);
        List<ImportJobErrorRecord> failedRows = sourceJobDetail.itemErrors().stream()
                .filter(this::isReplayableRow)
                .filter(error -> errorCodes.contains(error.errorCode()))
                .toList();
        if (failedRows.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "import job has no replayable failed rows for selected errorCodes");
        }
        return new ReplayableFailedRows(sourceJobDetail.job(), failedRows);
    }

    public EditedReplaySource loadEditedReplay(Long tenantId,
                                               Long sourceJobId,
                                               List<ImportReplayFileWriter.EditedReplayRowReplacement> editedRows) {
        ImportJobDetail sourceJobDetail = requireReplayableSourceJob(tenantId, sourceJobId);
        if (editedRows == null || editedRows.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "items must not be empty");
        }
        List<Long> errorIds = editedRows.stream().map(ImportReplayFileWriter.EditedReplayRowReplacement::errorId).toList();
        List<ImportJobErrorRecord> sourceErrors = sourceJobDetail.itemErrors().stream()
                .filter(error -> errorIds.contains(error.id()))
                .toList();
        if (sourceErrors.size() != editedRows.size()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "edited replay items must belong to source import job");
        }
        Map<Long, ImportReplayFileWriter.EditedReplayRowReplacement> editedRowsByErrorId = new LinkedHashMap<>();
        for (ImportReplayFileWriter.EditedReplayRowReplacement editedRow : editedRows) {
            editedRowsByErrorId.put(editedRow.errorId(), editedRow);
        }
        return new EditedReplaySource(sourceJobDetail.job(), sourceErrors, editedRowsByErrorId);
    }

    private ImportJobDetail requireReplayableSourceJob(Long tenantId, Long sourceJobId) {
        ImportJobDetail sourceJobDetail = importJobQueryUseCase.getJobDetail(tenantId, sourceJobId);
        validateReplayableSourceJob(sourceJobDetail.job());
        return sourceJobDetail;
    }

    private ImportJobDetail requireWholeFileReplayableSourceJob(Long tenantId, Long sourceJobId) {
        ImportJobDetail sourceJobDetail = importJobQueryUseCase.getJobDetail(tenantId, sourceJobId);
        validateWholeFileReplayableSourceJob(sourceJobDetail);
        return sourceJobDetail;
    }

    private void validateReplayableSourceJob(ImportJobRecord sourceJob) {
        if (!isTerminalStatus(sourceJob.status())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "import job must be in terminal status before replay");
        }
        if (!"USER_CSV".equals(sourceJob.importType())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "replay is only supported for USER_CSV");
        }
        if (sourceJob.failureCount() == null || sourceJob.failureCount() <= 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, "import job has no failed rows to replay");
        }
    }

    private void validateWholeFileReplayableSourceJob(ImportJobDetail sourceJobDetail) {
        ImportJobRecord sourceJob = sourceJobDetail.job();
        if (!"FAILED".equals(sourceJob.status())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "whole-file replay is only supported for FAILED source jobs");
        }
        if (!"USER_CSV".equals(sourceJob.importType())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "replay is only supported for USER_CSV");
        }
        if (safeInt(sourceJob.failureCount()) <= 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, "import job has no failed rows to replay");
        }
        if (safeInt(sourceJob.successCount()) > 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, "whole-file replay is only supported when source job has no successful rows");
        }
        long replayableRowErrors = sourceJobDetail.itemErrors().stream().filter(this::isReplayableRow).count();
        if (replayableRowErrors <= 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, "whole-file replay is only supported for row-level failed rows");
        }
    }

    private boolean isTerminalStatus(String status) {
        return "SUCCEEDED".equals(status) || "FAILED".equals(status);
    }

    private boolean isReplayableRow(ImportJobErrorRecord error) {
        return error.rowNumber() != null && error.rowNumber() > 1;
    }

    private int resolveWholeFileReplayRowCount(ImportJobRecord sourceJob) {
        int totalCount = safeInt(sourceJob.totalCount());
        if (totalCount > 0) {
            return totalCount;
        }
        return safeInt(sourceJob.failureCount());
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    public record ReplayableFailedRows(ImportJobRecord sourceJob, List<ImportJobErrorRecord> failedRows) {
    }

    public record WholeFileReplaySource(ImportJobRecord sourceJob, int replayRowCount) {
    }

    public record EditedReplaySource(
            ImportJobRecord sourceJob,
            List<ImportJobErrorRecord> sourceErrors,
            Map<Long, ImportReplayFileWriter.EditedReplayRowReplacement> editedRowsByErrorId
    ) {
    }
}
