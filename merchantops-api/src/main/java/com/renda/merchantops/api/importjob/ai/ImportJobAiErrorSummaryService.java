package com.renda.merchantops.api.importjob.ai;

import com.renda.merchantops.api.ai.core.AiInteractionExecutionSupport;
import com.renda.merchantops.api.ai.core.AiProviderException;
import com.renda.merchantops.api.ai.core.AiProviderFailureType;
import com.renda.merchantops.api.ai.importjob.errorsummary.ImportJobErrorSummaryAiProvider;
import com.renda.merchantops.api.ai.importjob.errorsummary.ImportJobErrorSummaryPrompt;
import com.renda.merchantops.api.ai.importjob.errorsummary.ImportJobErrorSummaryPromptBuilder;
import com.renda.merchantops.api.ai.importjob.errorsummary.ImportJobErrorSummaryPromptContext;
import com.renda.merchantops.api.ai.importjob.errorsummary.ImportJobErrorSummaryProviderRequest;
import com.renda.merchantops.api.ai.importjob.errorsummary.ImportJobErrorSummaryProviderResult;
import com.renda.merchantops.api.config.AiProperties;
import com.renda.merchantops.api.dto.importjob.query.ImportJobAiErrorSummaryResponse;
import com.renda.merchantops.api.importjob.ImportCsvSupport;
import com.renda.merchantops.domain.importjob.ImportJobDetail;
import com.renda.merchantops.domain.importjob.ImportJobErrorPageCriteria;
import com.renda.merchantops.domain.importjob.ImportJobErrorRecord;
import com.renda.merchantops.domain.importjob.ImportJobQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ImportJobAiErrorSummaryService {

    private static final String ENTITY_TYPE_IMPORT_JOB = "IMPORT_JOB";
    private static final String INTERACTION_TYPE_ERROR_SUMMARY = "ERROR_SUMMARY";
    private static final int PROMPT_WINDOW_PAGE = 0;
    private static final int PROMPT_WINDOW_SIZE = 20;

    private final ImportJobQueryUseCase importJobQueryUseCase;
    private final ImportJobErrorSummaryPromptBuilder importJobErrorSummaryPromptBuilder;
    private final ImportJobErrorSummaryAiProvider importJobErrorSummaryAiProvider;
    private final AiInteractionExecutionSupport aiInteractionExecutionSupport;
    private final AiProperties aiProperties;

    public ImportJobAiErrorSummaryResponse generateErrorSummary(Long tenantId, Long userId, String requestId, Long importJobId) {
        String normalizedRequestId = aiInteractionExecutionSupport.normalizeRequestId(requestId);
        ImportJobDetail detail = importJobQueryUseCase.getJobDetail(tenantId, importJobId);
        List<ImportJobErrorRecord> promptWindowErrors = importJobQueryUseCase.pageJobErrors(
                tenantId,
                importJobId,
                new ImportJobErrorPageCriteria(PROMPT_WINDOW_PAGE, PROMPT_WINDOW_SIZE, null)
        ).items();
        String promptVersion = aiInteractionExecutionSupport.normalizePromptVersion(
                aiProperties.getImportErrorSummaryPromptVersion(),
                "import-error-summary-v1"
        );
        String configuredModelId = aiInteractionExecutionSupport.normalizeNullable(aiProperties.resolveModelId());
        ImportJobErrorSummaryPromptContext promptContext = toPromptContext(detail, promptWindowErrors);
        ImportJobErrorSummaryPrompt prompt = importJobErrorSummaryPromptBuilder.build(promptVersion, promptContext);

        aiInteractionExecutionSupport.assertAvailable(
                tenantId,
                userId,
                normalizedRequestId,
                ENTITY_TYPE_IMPORT_JOB,
                importJobId,
                INTERACTION_TYPE_ERROR_SUMMARY,
                promptVersion,
                configuredModelId,
                aiProperties,
                "import ai error summary is disabled",
                "import ai error summary is unavailable"
        );

        long startedAt = System.nanoTime();
        try {
            ImportJobErrorSummaryProviderResult providerResult = importJobErrorSummaryAiProvider.generateErrorSummary(
                    new ImportJobErrorSummaryProviderRequest(
                            normalizedRequestId,
                            importJobId,
                            configuredModelId,
                            aiProperties.getTimeoutMs(),
                            prompt
                    )
            );
            long latencyMs = aiInteractionExecutionSupport.elapsedMillis(startedAt);
            String resolvedModelId = aiInteractionExecutionSupport.normalizeNullable(providerResult.modelId());
            String summary = normalizeRequiredText(providerResult.summary(), "summary");
            List<String> topErrorPatterns = normalizeRequiredItems(providerResult.topErrorPatterns(), "topErrorPatterns");
            List<String> recommendedNextSteps = normalizeRequiredItems(providerResult.recommendedNextSteps(), "recommendedNextSteps");

            aiInteractionExecutionSupport.recordSuccess(
                    tenantId,
                    userId,
                    normalizedRequestId,
                    ENTITY_TYPE_IMPORT_JOB,
                    importJobId,
                    INTERACTION_TYPE_ERROR_SUMMARY,
                    promptVersion,
                    resolvedModelId,
                    latencyMs,
                    summary,
                    providerResult
            );

            return new ImportJobAiErrorSummaryResponse(
                    importJobId,
                    summary,
                    topErrorPatterns,
                    recommendedNextSteps,
                    promptVersion,
                    resolvedModelId,
                    LocalDateTime.now(),
                    latencyMs,
                    normalizedRequestId
            );
        } catch (AiProviderException ex) {
            long latencyMs = aiInteractionExecutionSupport.elapsedMillis(startedAt);
            aiInteractionExecutionSupport.recordFailure(
                    tenantId,
                    userId,
                    normalizedRequestId,
                    ENTITY_TYPE_IMPORT_JOB,
                    importJobId,
                    INTERACTION_TYPE_ERROR_SUMMARY,
                    promptVersion,
                    configuredModelId,
                    ex.getFailureType(),
                    latencyMs
            );
            throw aiInteractionExecutionSupport.toBizException(ex, "import ai error summary timed out", "import ai error summary is unavailable");
        }
    }

    private ImportJobErrorSummaryPromptContext toPromptContext(ImportJobDetail detail, List<ImportJobErrorRecord> promptWindowErrors) {
        return new ImportJobErrorSummaryPromptContext(
                detail.job().id(),
                detail.job().importType(),
                detail.job().sourceType(),
                detail.job().sourceFilename(),
                detail.job().status(),
                detail.job().errorSummary(),
                detail.job().totalCount(),
                detail.job().successCount(),
                detail.job().failureCount(),
                detail.job().sourceJobId(),
                detail.job().createdAt(),
                detail.job().startedAt(),
                detail.job().finishedAt(),
                detail.errorCodeCounts().stream()
                        .map(item -> new ImportJobErrorSummaryPromptContext.ErrorCodeCount(item.errorCode(), item.count()))
                        .toList(),
                promptWindowErrors.stream().map(this::toErrorRowContext).toList()
        );
    }

    private ImportJobErrorSummaryPromptContext.ErrorRowContext toErrorRowContext(ImportJobErrorRecord error) {
        return new ImportJobErrorSummaryPromptContext.ErrorRowContext(
                error.rowNumber(),
                error.errorCode(),
                error.errorMessage(),
                summarizeRawPayload(error.rawPayload())
        );
    }

    private ImportJobErrorSummaryPromptContext.SanitizedRowSummary summarizeRawPayload(String rawPayload) {
        if (!StringUtils.hasText(rawPayload)) {
            return new ImportJobErrorSummaryPromptContext.SanitizedRowSummary(false, false, null, false, false, false, false, false, 0);
        }
        try (CSVParser parser = ImportCsvSupport.RAW_PAYLOAD_CSV_FORMAT.parse(new StringReader(rawPayload))) {
            List<CSVRecord> records = parser.getRecords();
            if (records.size() != 1) {
                return new ImportJobErrorSummaryPromptContext.SanitizedRowSummary(true, false, null, false, false, false, false, false, 0);
            }
            CSVRecord record = records.getFirst();
            String username = column(record, 0);
            String displayName = column(record, 1);
            String email = column(record, 2);
            String password = column(record, 3);
            String roleCodes = column(record, 4);
            return new ImportJobErrorSummaryPromptContext.SanitizedRowSummary(
                    true,
                    true,
                    record.size(),
                    StringUtils.hasText(username),
                    StringUtils.hasText(displayName),
                    StringUtils.hasText(email),
                    StringUtils.hasText(password),
                    StringUtils.hasText(roleCodes),
                    countRoleCodes(roleCodes)
            );
        } catch (IOException | RuntimeException ex) {
            return new ImportJobErrorSummaryPromptContext.SanitizedRowSummary(true, false, null, false, false, false, false, false, 0);
        }
    }

    private String column(CSVRecord record, int index) {
        if (record == null || index < 0 || index >= record.size()) {
            return null;
        }
        return record.get(index);
    }

    private int countRoleCodes(String roleCodes) {
        if (!StringUtils.hasText(roleCodes)) {
            return 0;
        }
        return (int) Arrays.stream(roleCodes.split("\\|", -1))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .count();
    }

    private String normalizeRequiredText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider import error summary payload is missing " + fieldName);
        }
        return value.trim();
    }

    private List<String> normalizeRequiredItems(List<String> items, String fieldName) {
        if (items == null || items.isEmpty()) {
            throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider import error summary payload is missing " + fieldName);
        }
        List<String> normalized = items.stream()
                .map(item -> item == null ? null : item.trim())
                .filter(StringUtils::hasText)
                .toList();
        if (normalized.size() != items.size()) {
            throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider import error summary payload has blank " + fieldName + " item");
        }
        return normalized;
    }
}
