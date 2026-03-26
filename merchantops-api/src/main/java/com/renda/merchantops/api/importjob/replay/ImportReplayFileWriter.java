package com.renda.merchantops.api.importjob.replay;

import com.renda.merchantops.api.importjob.ImportCsvSupport;
import com.renda.merchantops.api.importjob.ImportFileStorageService;
import com.renda.merchantops.domain.importjob.ImportJobErrorRecord;
import com.renda.merchantops.domain.importjob.ImportJobRecord;
import com.renda.merchantops.domain.shared.error.BizException;
import com.renda.merchantops.domain.shared.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

@Component
@RequiredArgsConstructor
public class ImportReplayFileWriter {

    private static final String REPLAY_FILENAME_PREFIX = "replay-failures-job-";
    private static final String WHOLE_FILE_REPLAY_FILENAME_PREFIX = "replay-file-job-";
    private static final String EDITED_REPLAY_FILENAME_PREFIX = "replay-edited-job-";

    private final ImportFileStorageService importFileStorageService;

    public ReplayFileBuildResult writeFailedRowReplay(Long tenantId,
                                                      ImportJobRecord sourceJob,
                                                      List<ImportJobErrorRecord> failedRows) {
        String filename = REPLAY_FILENAME_PREFIX + sourceJob.id() + ".csv";
        String csvContent = buildReplayCsv(failedRows);
        return storeReplayFile(tenantId, sourceJob, filename, csvContent, failedRows.size());
    }

    public ReplayFileBuildResult copyWholeFileReplay(Long tenantId,
                                                     ImportJobRecord sourceJob,
                                                     int replayRowCount) {
        String filename = WHOLE_FILE_REPLAY_FILENAME_PREFIX + sourceJob.id() + ".csv";
        try (InputStream inputStream = importFileStorageService.openStream(sourceJob.storageKey())) {
            String storageKey = importFileStorageService.store(tenantId, filename, inputStream);
            return new ReplayFileBuildResult(sourceJob, filename, storageKey, replayRowCount);
        } catch (IOException ex) {
            throw new BizException(ErrorCode.BIZ_ERROR, "failed to copy replay source file");
        }
    }

    public ReplayFileBuildResult writeEditedReplay(Long tenantId,
                                                   ImportJobRecord sourceJob,
                                                   List<ImportJobErrorRecord> sourceErrors,
                                                   Map<Long, EditedReplayRowReplacement> editedRowsByErrorId) {
        String filename = EDITED_REPLAY_FILENAME_PREFIX + sourceJob.id() + ".csv";
        String csvContent = buildEditedReplayCsv(sourceErrors, editedRowsByErrorId);
        return storeReplayFile(tenantId, sourceJob, filename, csvContent, sourceErrors.size());
    }

    private ReplayFileBuildResult storeReplayFile(Long tenantId,
                                                  ImportJobRecord sourceJob,
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

    private String buildReplayCsv(List<ImportJobErrorRecord> failedRows) {
        try (StringWriter writer = new StringWriter();
             CSVPrinter printer = new CSVPrinter(writer, ImportCsvSupport.RAW_PAYLOAD_CSV_FORMAT)) {
            printer.printRecord(ImportCsvSupport.USER_CSV_HEADERS);
            for (ImportJobErrorRecord failedRow : failedRows) {
                printer.printRecord(parseRawPayload(failedRow));
            }
            return writer.toString();
        } catch (IOException ex) {
            throw new BizException(ErrorCode.BIZ_ERROR, "failed to build replay csv");
        }
    }

    private String buildEditedReplayCsv(List<ImportJobErrorRecord> sourceErrors,
                                        Map<Long, EditedReplayRowReplacement> editedRowsByErrorId) {
        try (StringWriter writer = new StringWriter();
             CSVPrinter printer = new CSVPrinter(writer, ImportCsvSupport.RAW_PAYLOAD_CSV_FORMAT)) {
            printer.printRecord(ImportCsvSupport.USER_CSV_HEADERS);
            for (ImportJobErrorRecord sourceError : sourceErrors) {
                validateEditableSourceError(sourceError);
                EditedReplayRowReplacement editedRow = editedRowsByErrorId.get(sourceError.id());
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

    private List<String> parseRawPayload(ImportJobErrorRecord failedRow) throws IOException {
        if (!StringUtils.hasText(failedRow.rawPayload())) {
            throw new BizException(ErrorCode.BIZ_ERROR, "import job failed row payload is missing");
        }
        try (CSVParser parser = ImportCsvSupport.IMPORT_CSV_FORMAT.parse(new StringReader(failedRow.rawPayload()))) {
            List<CSVRecord> records = parser.getRecords();
            if (records.size() != 1) {
                throw new BizException(ErrorCode.BIZ_ERROR, "import job failed row payload is invalid");
            }
            return StreamSupport.stream(records.get(0).spliterator(), false).toList();
        }
    }

    private void validateEditableSourceError(ImportJobErrorRecord sourceError) {
        if (sourceError.rowNumber() == null || sourceError.rowNumber() <= 1 || !StringUtils.hasText(sourceError.rawPayload())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "edited replay is only supported for row-level errors with raw payload");
        }
    }

    public record ReplayFileBuildResult(
            ImportJobRecord sourceJob,
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
