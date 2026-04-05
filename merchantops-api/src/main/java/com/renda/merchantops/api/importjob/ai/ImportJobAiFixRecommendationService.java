package com.renda.merchantops.api.importjob.ai;

import com.renda.merchantops.api.ai.core.AiGenerationWorkflow;
import com.renda.merchantops.api.ai.core.AiInteractionExecutionSupport;
import com.renda.merchantops.api.ai.core.AiProviderException;
import com.renda.merchantops.api.ai.core.AiProviderFailureType;
import com.renda.merchantops.api.ai.importjob.fixrecommendation.ImportJobFixRecommendationAiProvider;
import com.renda.merchantops.api.ai.importjob.fixrecommendation.ImportJobFixRecommendationPrompt;
import com.renda.merchantops.api.ai.importjob.fixrecommendation.ImportJobFixRecommendationPromptBuilder;
import com.renda.merchantops.api.ai.importjob.fixrecommendation.ImportJobFixRecommendationPromptContext;
import com.renda.merchantops.api.ai.importjob.fixrecommendation.ImportJobFixRecommendationProviderRequest;
import com.renda.merchantops.api.ai.importjob.fixrecommendation.ImportJobFixRecommendationProviderResult;
import com.renda.merchantops.api.config.AiProperties;
import com.renda.merchantops.api.dto.importjob.query.ImportJobAiFixRecommendationResponse;
import com.renda.merchantops.api.importjob.ImportCsvSupport;
import com.renda.merchantops.domain.importjob.ImportJobDetail;
import com.renda.merchantops.domain.importjob.ImportJobErrorCount;
import com.renda.merchantops.domain.importjob.ImportJobErrorPageCriteria;
import com.renda.merchantops.domain.importjob.ImportJobErrorRecord;
import com.renda.merchantops.domain.importjob.ImportJobQueryUseCase;
import com.renda.merchantops.domain.shared.error.BizException;
import com.renda.merchantops.domain.shared.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ImportJobAiFixRecommendationService {

    private static final AiGenerationWorkflow WORKFLOW = AiGenerationWorkflow.IMPORT_FIX_RECOMMENDATION;
    private static final String IMPORT_TYPE_USER_CSV = "USER_CSV";
    private static final int PROMPT_WINDOW_PAGE = 0;
    private static final int PROMPT_WINDOW_SIZE = 20;
    private static final int MAX_GROUPS = 5;
    private static final int MAX_SAMPLE_ROWS_PER_GROUP = 3;
    private static final int MAX_SAMPLE_ERROR_MESSAGES = 3;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("\\b[\\w.%-]+@[\\w.-]+\\.[A-Za-z]{2,}\\b");
    private static final Pattern RAW_CSV_PATTERN = Pattern.compile("[^,\\n]+,[^,\\n]+,[^,\\n]+,[^,\\n]+,[^,\\n]+");

    private final ImportJobQueryUseCase importJobQueryUseCase;
    private final ImportJobFixRecommendationPromptBuilder importJobFixRecommendationPromptBuilder;
    private final ImportJobFixRecommendationAiProvider importJobFixRecommendationAiProvider;
    private final AiInteractionExecutionSupport aiInteractionExecutionSupport;
    private final AiProperties aiProperties;

    public ImportJobAiFixRecommendationResponse generateFixRecommendation(Long tenantId,
                                                                         Long userId,
                                                                         String requestId,
                                                                         Long importJobId) {
        String normalizedRequestId = aiInteractionExecutionSupport.normalizeRequestId(requestId);
        ImportJobDetail detail = importJobQueryUseCase.getJobDetail(tenantId, importJobId);
        assertSupportedImportType(detail.job().importType());
        assertFailureSignals(detail);

        List<ImportJobErrorRecord> promptWindowErrors = importJobQueryUseCase.pageJobErrors(
                tenantId,
                importJobId,
                new ImportJobErrorPageCriteria(PROMPT_WINDOW_PAGE, PROMPT_WINDOW_SIZE, null)
        ).items().stream()
                .filter(error -> error != null && error.rowNumber() != null)
                .toList();
        List<GroundedErrorGroup> groundedErrorGroups = extractGroundedErrorGroups(detail, promptWindowErrors);
        Set<String> sensitiveTokens = collectSensitiveTokens(detail.itemErrors(), promptWindowErrors);
        String promptVersion = WORKFLOW.resolvePromptVersion(aiProperties, aiInteractionExecutionSupport);
        String configuredModelId = aiInteractionExecutionSupport.normalizeNullable(aiProperties.resolveModelId());
        ImportJobFixRecommendationPromptContext promptContext = toPromptContext(detail, groundedErrorGroups);
        ImportJobFixRecommendationPrompt prompt = importJobFixRecommendationPromptBuilder.build(promptVersion, promptContext);

        aiInteractionExecutionSupport.assertAvailable(
                tenantId,
                userId,
                normalizedRequestId,
                WORKFLOW.entityType(),
                importJobId,
                WORKFLOW.interactionType(),
                promptVersion,
                configuredModelId,
                aiProperties,
                "import ai fix recommendation is disabled",
                "import ai fix recommendation is unavailable"
        );

        long startedAt = System.nanoTime();
        try {
            ImportJobFixRecommendationProviderResult providerResult = importJobFixRecommendationAiProvider.generateFixRecommendation(
                    new ImportJobFixRecommendationProviderRequest(
                            normalizedRequestId,
                            importJobId,
                            configuredModelId,
                            aiProperties.getTimeoutMs(),
                            prompt
                    )
            );
            long latencyMs = aiInteractionExecutionSupport.elapsedMillis(startedAt);
            String resolvedModelId = aiInteractionExecutionSupport.normalizeNullable(providerResult.modelId());
            String summary = normalizeSafeText(providerResult.summary(), "summary", sensitiveTokens);
            List<String> orderedErrorCodes = groundedErrorGroups.stream().map(GroundedErrorGroup::errorCode).toList();
            Map<String, Long> affectedRowsByErrorCode = new LinkedHashMap<>();
            groundedErrorGroups.forEach(group -> affectedRowsByErrorCode.put(group.errorCode(), group.affectedRowsEstimate()));
            List<ImportJobAiFixRecommendationResponse.RecommendedFix> recommendedFixes = normalizeRecommendedFixes(
                    providerResult.recommendedFixes(),
                    orderedErrorCodes,
                    affectedRowsByErrorCode,
                    sensitiveTokens
            );
            List<String> confidenceNotes = normalizeSafeItems(providerResult.confidenceNotes(), "confidenceNotes", sensitiveTokens);
            List<String> recommendedOperatorChecks = normalizeSafeItems(
                    providerResult.recommendedOperatorChecks(),
                    "recommendedOperatorChecks",
                    sensitiveTokens
            );

            aiInteractionExecutionSupport.recordSuccess(
                    tenantId,
                    userId,
                    normalizedRequestId,
                    WORKFLOW.entityType(),
                    importJobId,
                    WORKFLOW.interactionType(),
                    promptVersion,
                    resolvedModelId,
                    latencyMs,
                    summary,
                    providerResult
            );

            return new ImportJobAiFixRecommendationResponse(
                    importJobId,
                    summary,
                    recommendedFixes,
                    confidenceNotes,
                    recommendedOperatorChecks,
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
                    WORKFLOW.entityType(),
                    importJobId,
                    WORKFLOW.interactionType(),
                    promptVersion,
                    configuredModelId,
                    ex.getFailureType(),
                    latencyMs
            );
            throw aiInteractionExecutionSupport.toBizException(ex, "import ai fix recommendation timed out", "import ai fix recommendation is unavailable");
        }
    }

    private void assertSupportedImportType(String importType) {
        if (!IMPORT_TYPE_USER_CSV.equals(importType)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "import ai fix recommendation only supports USER_CSV");
        }
    }

    private void assertFailureSignals(ImportJobDetail detail) {
        boolean hasFailureSignals = hasPositive(detail.job().failureCount())
                || hasPositiveCounts(detail.errorCodeCounts())
                || (detail.itemErrors() != null && !detail.itemErrors().isEmpty());
        if (!hasFailureSignals) {
            throw new BizException(ErrorCode.BAD_REQUEST, "import job has no failure signals for fix recommendation");
        }
    }

    private boolean hasPositive(Integer value) {
        return value != null && value > 0;
    }

    private boolean hasPositiveCounts(List<ImportJobErrorCount> errorCodeCounts) {
        return errorCodeCounts != null
                && errorCodeCounts.stream().anyMatch(item -> item != null && item.count() > 0);
    }

    private List<GroundedErrorGroup> extractGroundedErrorGroups(ImportJobDetail detail,
                                                                List<ImportJobErrorRecord> promptWindowErrors) {
        if (promptWindowErrors == null || promptWindowErrors.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "import job has no sanitized row signal for fix recommendation");
        }
        Map<String, Long> totalCountsByErrorCode = new LinkedHashMap<>();
        if (detail.errorCodeCounts() != null) {
            detail.errorCodeCounts().stream()
                    .filter(item -> item != null && StringUtils.hasText(item.errorCode()) && item.count() > 0)
                    .forEach(item -> totalCountsByErrorCode.put(item.errorCode().trim(), item.count()));
        }
        Map<String, List<ImportJobErrorRecord>> grouped = new LinkedHashMap<>();
        promptWindowErrors.stream()
                .filter(error -> StringUtils.hasText(error.errorCode()))
                .forEach(error -> grouped.computeIfAbsent(error.errorCode().trim(), key -> new ArrayList<>()).add(error));
        if (grouped.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "import job has no sanitized row signal for fix recommendation");
        }
        return grouped.entrySet().stream()
                .map(entry -> new GroundedErrorGroup(
                        entry.getKey(),
                        totalCountsByErrorCode.getOrDefault(entry.getKey(), (long) entry.getValue().size()),
                        entry.getValue().stream()
                                .sorted(Comparator.comparing(ImportJobErrorRecord::rowNumber))
                                .limit(MAX_SAMPLE_ROWS_PER_GROUP)
                                .toList()
                ))
                .sorted(Comparator.comparingLong(GroundedErrorGroup::affectedRowsEstimate).reversed()
                        .thenComparing(GroundedErrorGroup::errorCode))
                .limit(MAX_GROUPS)
                .toList();
    }

    private ImportJobFixRecommendationPromptContext toPromptContext(ImportJobDetail detail,
                                                                    List<GroundedErrorGroup> groundedErrorGroups) {
        return new ImportJobFixRecommendationPromptContext(
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
                        .map(item -> new ImportJobFixRecommendationPromptContext.ErrorCodeCount(item.errorCode(), item.count()))
                        .toList(),
                groundedErrorGroups.stream().map(this::toErrorGroupContext).toList()
        );
    }

    private ImportJobFixRecommendationPromptContext.ErrorGroupContext toErrorGroupContext(GroundedErrorGroup group) {
        List<String> sampleErrorMessages = group.sampleRows().stream()
                .map(ImportJobErrorRecord::errorMessage)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .limit(MAX_SAMPLE_ERROR_MESSAGES)
                .toList();
        return new ImportJobFixRecommendationPromptContext.ErrorGroupContext(
                group.errorCode(),
                group.affectedRowsEstimate(),
                group.sampleRows().size(),
                sampleErrorMessages,
                group.sampleRows().stream().map(this::toErrorRowContext).toList()
        );
    }

    private ImportJobFixRecommendationPromptContext.ErrorRowContext toErrorRowContext(ImportJobErrorRecord error) {
        ImportJobAiSanitizationSupport.SanitizedRowSummary rowSummary = ImportJobAiSanitizationSupport.summarizeRowPayload(error.rawPayload());
        return new ImportJobFixRecommendationPromptContext.ErrorRowContext(
                error.rowNumber(),
                error.errorMessage(),
                new ImportJobFixRecommendationPromptContext.SanitizedRowSummary(
                        rowSummary.rawPayloadPresent(),
                        rowSummary.rawPayloadParsed(),
                        rowSummary.columnCount(),
                        rowSummary.usernamePresent(),
                        rowSummary.displayNamePresent(),
                        rowSummary.emailPresent(),
                        rowSummary.passwordPresent(),
                        rowSummary.roleCodesPresent(),
                        rowSummary.roleCodeCount()
                )
        );
    }

    private List<ImportJobAiFixRecommendationResponse.RecommendedFix> normalizeRecommendedFixes(
            List<ImportJobFixRecommendationProviderResult.RecommendedFix> providerFixes,
            List<String> orderedErrorCodes,
            Map<String, Long> affectedRowsByErrorCode,
            Set<String> sensitiveTokens
    ) {
        if (providerFixes == null || providerFixes.isEmpty()) {
            throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider import fix recommendation payload is missing recommendedFixes");
        }
        if (providerFixes.size() > MAX_GROUPS) {
            throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider import fix recommendation payload has too many recommendedFixes");
        }
        Map<String, ImportJobAiFixRecommendationResponse.RecommendedFix> normalizedByErrorCode = new LinkedHashMap<>();
        for (ImportJobFixRecommendationProviderResult.RecommendedFix item : providerFixes) {
            if (item == null) {
                throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider import fix recommendation payload has invalid recommendedFixes item");
            }
            String errorCode = normalizeRequiredText(item.errorCode(), "recommendedFixes.errorCode");
            if (!affectedRowsByErrorCode.containsKey(errorCode)) {
                throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider import fix recommendation payload has unsupported recommendedFixes.errorCode " + errorCode);
            }
            if (normalizedByErrorCode.containsKey(errorCode)) {
                throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider import fix recommendation payload has duplicate recommendedFixes.errorCode " + errorCode);
            }
            String recommendedAction = normalizeSafeText(item.recommendedAction(), "recommendedFixes.recommendedAction", sensitiveTokens);
            String reasoning = normalizeSafeText(item.reasoning(), "recommendedFixes.reasoning", sensitiveTokens);
            normalizedByErrorCode.put(errorCode, new ImportJobAiFixRecommendationResponse.RecommendedFix(
                    errorCode,
                    recommendedAction,
                    reasoning,
                    item.reviewRequired(),
                    affectedRowsByErrorCode.get(errorCode)
            ));
        }
        List<ImportJobAiFixRecommendationResponse.RecommendedFix> normalized = orderedErrorCodes.stream()
                .filter(normalizedByErrorCode::containsKey)
                .map(normalizedByErrorCode::get)
                .toList();
        if (normalized.isEmpty()) {
            throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider import fix recommendation payload is missing recommendedFixes");
        }
        return normalized;
    }

    private List<String> normalizeSafeItems(List<String> items, String fieldName, Set<String> sensitiveTokens) {
        if (items == null || items.isEmpty()) {
            throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider import fix recommendation payload is missing " + fieldName);
        }
        List<String> normalized = new ArrayList<>();
        for (String item : items) {
            normalized.add(normalizeSafeText(item, fieldName, sensitiveTokens));
        }
        return normalized;
    }

    private String normalizeSafeText(String value, String fieldName, Set<String> sensitiveTokens) {
        String normalized = normalizeRequiredText(value, fieldName);
        assertNoSensitiveEcho(normalized, fieldName, sensitiveTokens);
        return normalized;
    }

    private String normalizeRequiredText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider import fix recommendation payload is missing " + fieldName);
        }
        return value.trim();
    }

    private void assertNoSensitiveEcho(String value, String fieldName, Set<String> sensitiveTokens) {
        String normalized = value.toLowerCase(Locale.ROOT);
        if (EMAIL_PATTERN.matcher(normalized).find() || RAW_CSV_PATTERN.matcher(normalized).find()) {
            throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider import fix recommendation payload echoed sensitive row value in " + fieldName);
        }
        for (String sensitiveToken : sensitiveTokens) {
            if (normalized.contains(sensitiveToken)) {
                throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider import fix recommendation payload echoed sensitive row value in " + fieldName);
            }
        }
    }

    private Set<String> collectSensitiveTokens(List<ImportJobErrorRecord> detailItemErrors,
                                               List<ImportJobErrorRecord> promptWindowErrors) {
        Set<String> sensitiveTokens = new LinkedHashSet<>();
        collectSensitiveTokensFromErrors(detailItemErrors, sensitiveTokens);
        collectSensitiveTokensFromErrors(promptWindowErrors, sensitiveTokens);
        return sensitiveTokens;
    }

    private void collectSensitiveTokensFromErrors(List<ImportJobErrorRecord> errors, Set<String> sensitiveTokens) {
        if (errors == null || errors.isEmpty()) {
            return;
        }
        errors.stream()
                .filter(error -> error != null && error.rowNumber() != null)
                .forEach(error -> collectSensitiveTokensFromRawPayload(error.rawPayload(), sensitiveTokens));
    }

    private void collectSensitiveTokensFromRawPayload(String rawPayload, Set<String> sensitiveTokens) {
        if (!StringUtils.hasText(rawPayload)) {
            return;
        }
        addSensitiveToken(sensitiveTokens, rawPayload);
        try (CSVParser parser = ImportCsvSupport.RAW_PAYLOAD_CSV_FORMAT.parse(new StringReader(rawPayload))) {
            List<CSVRecord> records = parser.getRecords();
            if (records.size() != 1) {
                return;
            }
            CSVRecord record = records.getFirst();
            addSensitiveToken(sensitiveTokens, column(record, 0));
            addSensitiveToken(sensitiveTokens, column(record, 1));
            addSensitiveToken(sensitiveTokens, column(record, 2));
            addSensitiveToken(sensitiveTokens, column(record, 3));
            String roleCodes = column(record, 4);
            addSensitiveToken(sensitiveTokens, roleCodes);
            if (StringUtils.hasText(roleCodes)) {
                for (String roleCode : roleCodes.split("\\|", -1)) {
                    addSensitiveToken(sensitiveTokens, roleCode);
                }
            }
        } catch (IOException | RuntimeException ignored) {
            // No extra token extraction when raw payload cannot be parsed locally.
        }
    }

    private void addSensitiveToken(Set<String> sensitiveTokens, String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() < 3) {
            return;
        }
        sensitiveTokens.add(normalized);
    }

    private String column(CSVRecord record, int index) {
        if (record == null || index < 0 || index >= record.size()) {
            return null;
        }
        return record.get(index);
    }

    private record GroundedErrorGroup(
            String errorCode,
            long affectedRowsEstimate,
            List<ImportJobErrorRecord> sampleRows
    ) {
    }
}
