package com.renda.merchantops.api.service;

import com.renda.merchantops.common.exception.BizException;
import com.renda.merchantops.common.exception.ErrorCode;
import com.renda.merchantops.infra.persistence.entity.ImportJobEntity;
import com.renda.merchantops.infra.persistence.entity.ImportJobItemErrorEntity;
import com.renda.merchantops.infra.repository.ImportJobItemErrorRepository;
import com.renda.merchantops.infra.repository.ImportJobRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class ImportReplayFileBuilder {

    private static final String REPLAY_FILENAME_PREFIX = "replay-failures-job-";
    private static final String EDITED_REPLAY_FILENAME_PREFIX = "replay-edited-job-";

    private final ImportJobRepository importJobRepository;
    private final ImportJobItemErrorRepository importJobItemErrorRepository;
    private final ImportFileStorageService importFileStorageService;

    public ReplayFileBuildResult buildFailedRowReplay(Long tenantId, Long sourceJobId) {
        ImportJobEntity sourceJob = requireReplayableSourceJob(tenantId, sourceJobId);
        List<ImportJobItemErrorEntity> failedRows = importJobItemErrorRepository
                .findReplayableRowsByTenantIdAndImportJobId(tenantId, sourceJobId);
        if (failedRows.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "import job has no replayable failed rows");
        }

        return buildReplayFile(tenantId, sourceJob, failedRows);
    }

    public ReplayFileBuildResult buildSelectiveFailedRowReplay(Long tenantId,
                                                               Long sourceJobId,
                                                               List<String> errorCodes) {
        ImportJobEntity sourceJob = requireReplayableSourceJob(tenantId, sourceJobId);
        List<String> normalizedErrorCodes = normalizeSelectedErrorCodes(errorCodes);
        List<ImportJobItemErrorEntity> failedRows = importJobItemErrorRepository
                .findReplayableRowsByTenantIdAndImportJobIdAndErrorCodeIn(tenantId, sourceJobId, normalizedErrorCodes);
        if (failedRows.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "import job has no replayable failed rows for selected errorCodes");
        }

        return buildReplayFile(tenantId, sourceJob, failedRows);
    }

    public ReplayFileBuildResult buildEditedFailedRowReplay(Long tenantId,
                                                            Long sourceJobId,
                                                            List<EditedReplayRowReplacement> editedRows) {
        ImportJobEntity sourceJob = requireReplayableSourceJob(tenantId, sourceJobId);
        List<EditedReplayRowReplacement> normalizedEditedRows = requireEditedRows(editedRows);
        List<Long> errorIds = normalizedEditedRows.stream()
                .map(EditedReplayRowReplacement::errorId)
                .toList();
        List<ImportJobItemErrorEntity> sourceErrors = importJobItemErrorRepository
                .findAllByTenantIdAndImportJobIdAndIdInOrderByRowNumberAscIdAsc(tenantId, sourceJobId, errorIds);
        if (sourceErrors.size() != normalizedEditedRows.size()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "edited replay items must belong to source import job");
        }

        Map<Long, EditedReplayRowReplacement> editedRowsByErrorId = new LinkedHashMap<>();
        for (EditedReplayRowReplacement editedRow : normalizedEditedRows) {
            editedRowsByErrorId.put(editedRow.errorId(), editedRow);
        }

        String filename = EDITED_REPLAY_FILENAME_PREFIX + sourceJob.getId() + ".csv";
        String csvContent = buildEditedReplayCsv(sourceErrors, editedRowsByErrorId);
        return storeReplayFile(tenantId, sourceJob, filename, csvContent, sourceErrors.size());
    }

    private ImportJobEntity requireReplayableSourceJob(Long tenantId, Long sourceJobId) {
        ImportJobEntity sourceJob = importJobRepository.findByIdAndTenantId(sourceJobId, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "import job not found"));

        validateReplayableSourceJob(sourceJob);
        return sourceJob;
    }

    private ReplayFileBuildResult buildReplayFile(Long tenantId,
                                                  ImportJobEntity sourceJob,
                                                  List<ImportJobItemErrorEntity> failedRows) {
        String filename = REPLAY_FILENAME_PREFIX + sourceJob.getId() + ".csv";
        String csvContent = buildReplayCsv(failedRows);
        return storeReplayFile(tenantId, sourceJob, filename, csvContent, failedRows.size());
    }

    private ReplayFileBuildResult storeReplayFile(Long tenantId,
                                                  ImportJobEntity sourceJob,
                                                  String filename,
                                                  String csvContent,
                                                  int replayRowCount) {
        String storageKey = importFileStorageService.store(
                tenantId,
                filename,
                new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8))
        );
        return new ReplayFileBuildResult(sourceJob, filename, storageKey, replayRowCount);
    }

    private List<String> normalizeSelectedErrorCodes(List<String> errorCodes) {
        if (errorCodes == null || errorCodes.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "errorCodes must not be empty");
        }
        List<String> normalized = new ArrayList<>();
        for (String errorCode : errorCodes) {
            if (!StringUtils.hasText(errorCode)) {
                throw new BizException(ErrorCode.BAD_REQUEST, "errorCodes must not contain blank values");
            }
            String trimmed = errorCode.trim();
            if (!normalized.contains(trimmed)) {
                normalized.add(trimmed);
            }
        }
        if (normalized.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "errorCodes must not be empty");
        }
        return normalized;
    }

    private void validateReplayableSourceJob(ImportJobEntity sourceJob) {
        if (!isTerminalStatus(sourceJob.getStatus())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "import job must be in terminal status before replay");
        }
        if (!"USER_CSV".equals(sourceJob.getImportType())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "replay is only supported for USER_CSV");
        }
        if (sourceJob.getFailureCount() == null || sourceJob.getFailureCount() <= 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, "import job has no failed rows to replay");
        }
    }

    private boolean isTerminalStatus(String status) {
        return "SUCCEEDED".equals(status) || "FAILED".equals(status);
    }

    private String buildReplayCsv(List<ImportJobItemErrorEntity> failedRows) {
        try (StringWriter writer = new StringWriter();
             CSVPrinter printer = new CSVPrinter(writer, ImportCsvSupport.RAW_PAYLOAD_CSV_FORMAT)) {
            printer.printRecord(ImportCsvSupport.USER_CSV_HEADERS);
            for (ImportJobItemErrorEntity failedRow : failedRows) {
                printer.printRecord(parseRawPayload(failedRow));
            }
            return writer.toString();
        } catch (IOException ex) {
            throw new BizException(ErrorCode.BIZ_ERROR, "failed to build replay csv");
        }
    }

    private String buildEditedReplayCsv(List<ImportJobItemErrorEntity> sourceErrors,
                                        Map<Long, EditedReplayRowReplacement> editedRowsByErrorId) {
        try (StringWriter writer = new StringWriter();
             CSVPrinter printer = new CSVPrinter(writer, ImportCsvSupport.RAW_PAYLOAD_CSV_FORMAT)) {
            printer.printRecord(ImportCsvSupport.USER_CSV_HEADERS);
            for (ImportJobItemErrorEntity sourceError : sourceErrors) {
                validateEditableSourceError(sourceError);
                EditedReplayRowReplacement editedRow = editedRowsByErrorId.get(sourceError.getId());
                if (editedRow == null) {
                    throw new BizException(ErrorCode.BAD_REQUEST, "edited replay items must belong to source import job");
                }
                printer.printRecord(List.of(
                        editedRow.username(),
                        editedRow.displayName(),
                        editedRow.email(),
                        editedRow.password(),
                        String.join("|", editedRow.roleCodes())
                ));
            }
            return writer.toString();
        } catch (IOException ex) {
            throw new BizException(ErrorCode.BIZ_ERROR, "failed to build replay csv");
        }
    }

    private List<String> parseRawPayload(ImportJobItemErrorEntity failedRow) throws IOException {
        if (!StringUtils.hasText(failedRow.getRawPayload())) {
            throw new BizException(ErrorCode.BIZ_ERROR, "import job failed row payload is missing");
        }
        try (CSVParser parser = ImportCsvSupport.IMPORT_CSV_FORMAT.parse(new StringReader(failedRow.getRawPayload()))) {
            List<CSVRecord> records = parser.getRecords();
            if (records.size() != 1) {
                throw new BizException(ErrorCode.BIZ_ERROR, "import job failed row payload is invalid");
            }
            return StreamSupport.stream(records.get(0).spliterator(), false)
                    .toList();
        }
    }

    private List<EditedReplayRowReplacement> requireEditedRows(List<EditedReplayRowReplacement> editedRows) {
        if (editedRows == null || editedRows.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "items must not be empty");
        }
        return editedRows;
    }

    private void validateEditableSourceError(ImportJobItemErrorEntity sourceError) {
        if (sourceError.getRowNumber() == null || sourceError.getRowNumber() <= 1
                || !StringUtils.hasText(sourceError.getRawPayload())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "edited replay is only supported for row-level errors with raw payload");
        }
    }

    public record ReplayFileBuildResult(
            ImportJobEntity sourceJob,
            String sourceFilename,
            String storageKey,
            int replayRowCount
    ) {
    }

    public record EditedReplayRowReplacement(
            Long errorId,
            String username,
            String displayName,
            String email,
            String password,
            List<String> roleCodes
    ) {
    }
}
