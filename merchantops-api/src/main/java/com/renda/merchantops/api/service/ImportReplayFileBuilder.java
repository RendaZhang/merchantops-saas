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
import java.util.List;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class ImportReplayFileBuilder {

    private static final String REPLAY_FILENAME_PREFIX = "replay-failures-job-";

    private final ImportJobRepository importJobRepository;
    private final ImportJobItemErrorRepository importJobItemErrorRepository;
    private final ImportFileStorageService importFileStorageService;

    public ReplayFileBuildResult buildFailedRowReplay(Long tenantId, Long sourceJobId) {
        ImportJobEntity sourceJob = importJobRepository.findByIdAndTenantId(sourceJobId, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "import job not found"));

        validateReplayableSourceJob(sourceJob);
        List<ImportJobItemErrorEntity> failedRows = importJobItemErrorRepository
                .findReplayableRowsByTenantIdAndImportJobId(tenantId, sourceJobId);
        if (failedRows.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "import job has no replayable failed rows");
        }

        String filename = REPLAY_FILENAME_PREFIX + sourceJob.getId() + ".csv";
        String csvContent = buildReplayCsv(failedRows);
        String storageKey = importFileStorageService.store(
                tenantId,
                filename,
                new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8))
        );
        return new ReplayFileBuildResult(sourceJob, filename, storageKey, failedRows.size());
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

    public record ReplayFileBuildResult(
            ImportJobEntity sourceJob,
            String sourceFilename,
            String storageKey,
            int replayRowCount
    ) {
    }
}
