package com.renda.merchantops.api.importjob.ai;

import com.renda.merchantops.api.ai.core.AiGenerationWorkflow;
import com.renda.merchantops.api.ai.core.AiInteractionExecutionSupport;
import com.renda.merchantops.api.ai.core.AiProviderException;
import com.renda.merchantops.api.ai.core.AiProviderFailureType;
import com.renda.merchantops.api.ai.importjob.mappingsuggestion.ImportJobMappingSuggestionAiProvider;
import com.renda.merchantops.api.ai.importjob.mappingsuggestion.ImportJobMappingSuggestionPrompt;
import com.renda.merchantops.api.ai.importjob.mappingsuggestion.ImportJobMappingSuggestionPromptBuilder;
import com.renda.merchantops.api.ai.importjob.mappingsuggestion.ImportJobMappingSuggestionPromptContext;
import com.renda.merchantops.api.ai.importjob.mappingsuggestion.ImportJobMappingSuggestionProviderRequest;
import com.renda.merchantops.api.ai.importjob.mappingsuggestion.ImportJobMappingSuggestionProviderResult;
import com.renda.merchantops.api.config.AiProperties;
import com.renda.merchantops.api.dto.importjob.query.ImportJobAiMappingSuggestionResponse;
import com.renda.merchantops.api.featureflag.FeatureFlagGateService;
import com.renda.merchantops.api.importjob.ImportCsvSupport;
import com.renda.merchantops.domain.featureflag.FeatureFlagKey;
import com.renda.merchantops.domain.importjob.ImportJobDetail;
import com.renda.merchantops.domain.importjob.ImportJobErrorCount;
import com.renda.merchantops.domain.importjob.ImportJobErrorPageCriteria;
import com.renda.merchantops.domain.importjob.ImportJobErrorRecord;
import com.renda.merchantops.domain.importjob.ImportJobQueryUseCase;
import com.renda.merchantops.domain.shared.error.BizException;
import com.renda.merchantops.domain.shared.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ImportJobAiMappingSuggestionService {

    private static final AiGenerationWorkflow WORKFLOW = AiGenerationWorkflow.IMPORT_MAPPING_SUGGESTION;
    private static final String IMPORT_TYPE_USER_CSV = "USER_CSV";
    private static final int PROMPT_WINDOW_PAGE = 0;
    private static final int PROMPT_WINDOW_SIZE = 20;

    private final ImportJobQueryUseCase importJobQueryUseCase;
    private final ImportJobMappingSuggestionPromptBuilder importJobMappingSuggestionPromptBuilder;
    private final ImportJobMappingSuggestionAiProvider importJobMappingSuggestionAiProvider;
    private final AiInteractionExecutionSupport aiInteractionExecutionSupport;
    private final FeatureFlagGateService featureFlagGateService;
    private final AiProperties aiProperties;

    public ImportJobAiMappingSuggestionResponse generateMappingSuggestion(Long tenantId, Long userId, String requestId, Long importJobId) {
        String normalizedRequestId = aiInteractionExecutionSupport.normalizeRequestId(requestId);
        ImportJobDetail detail = importJobQueryUseCase.getJobDetail(tenantId, importJobId);
        assertSupportedImportType(detail.job().importType());
        assertFailureSignals(detail);
        HeaderSignalCandidate headerSignalCandidate = extractHeaderSignal(detail.itemErrors());

        List<ImportJobErrorRecord> promptWindowErrors = importJobQueryUseCase.pageJobErrors(
                tenantId,
                importJobId,
                new ImportJobErrorPageCriteria(PROMPT_WINDOW_PAGE, PROMPT_WINDOW_SIZE, null)
        ).items().stream()
                .filter(error -> error.rowNumber() != null)
                .toList();

        String promptVersion = WORKFLOW.resolvePromptVersion(aiProperties, aiInteractionExecutionSupport);
        String configuredModelId = aiInteractionExecutionSupport.normalizeNullable(aiProperties.resolveModelId());
        ImportJobMappingSuggestionPromptContext promptContext = toPromptContext(detail, headerSignalCandidate, promptWindowErrors);
        ImportJobMappingSuggestionPrompt prompt = importJobMappingSuggestionPromptBuilder.build(promptVersion, promptContext);

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
                featureFlagGateService.isEnabled(tenantId, FeatureFlagKey.AI_IMPORT_MAPPING_SUGGESTION),
                "import ai mapping suggestion is disabled",
                "import ai mapping suggestion is unavailable"
        );

        long startedAt = System.nanoTime();
        try {
            ImportJobMappingSuggestionProviderResult providerResult = importJobMappingSuggestionAiProvider.generateMappingSuggestion(
                    new ImportJobMappingSuggestionProviderRequest(
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
            List<ImportJobAiMappingSuggestionResponse.SuggestedFieldMapping> suggestedFieldMappings =
                    normalizeMappings(providerResult.suggestedFieldMappings(), headerSignalCandidate.headerSignal());
            List<String> confidenceNotes = normalizeRequiredItems(providerResult.confidenceNotes(), "confidenceNotes");
            List<String> recommendedOperatorChecks = normalizeRequiredItems(providerResult.recommendedOperatorChecks(), "recommendedOperatorChecks");

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

            return new ImportJobAiMappingSuggestionResponse(
                    importJobId,
                    summary,
                    suggestedFieldMappings,
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
            throw aiInteractionExecutionSupport.toBizException(ex, "import ai mapping suggestion timed out", "import ai mapping suggestion is unavailable");
        }
    }

    private void assertSupportedImportType(String importType) {
        if (!IMPORT_TYPE_USER_CSV.equals(importType)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "import ai mapping suggestion only supports USER_CSV");
        }
    }

    private void assertFailureSignals(ImportJobDetail detail) {
        boolean hasFailureSignals = hasPositive(detail.job().failureCount())
                || hasPositiveCounts(detail.errorCodeCounts())
                || (detail.itemErrors() != null && !detail.itemErrors().isEmpty());
        if (!hasFailureSignals) {
            throw new BizException(ErrorCode.BAD_REQUEST, "import job has no failure signals for mapping suggestion");
        }
    }

    private boolean hasPositive(Integer value) {
        return value != null && value > 0;
    }

    private boolean hasPositiveCounts(List<ImportJobErrorCount> errorCodeCounts) {
        return errorCodeCounts != null
                && errorCodeCounts.stream().anyMatch(item -> item != null && item.count() > 0);
    }

    private HeaderSignalCandidate extractHeaderSignal(List<ImportJobErrorRecord> itemErrors) {
        if (itemErrors != null) {
            for (ImportJobErrorRecord error : itemErrors) {
                if (error == null || error.rowNumber() != null) {
                    continue;
                }
                ImportJobAiSanitizationSupport.SanitizedHeaderSignal headerSignal =
                        ImportJobAiSanitizationSupport.summarizeHeaderSignal(error.rawPayload());
                if (headerSignal.rawPayloadParsed()
                        && headerSignal.headerColumnCount() != null
                        && headerSignal.observedColumns() != null
                        && !headerSignal.observedColumns().isEmpty()) {
                    return new HeaderSignalCandidate(error, headerSignal);
                }
            }
        }
        throw new BizException(ErrorCode.BAD_REQUEST, "import job has no sanitized header signal for mapping suggestion");
    }

    private ImportJobMappingSuggestionPromptContext toPromptContext(ImportJobDetail detail,
                                                                    HeaderSignalCandidate headerSignalCandidate,
                                                                    List<ImportJobErrorRecord> promptWindowErrors) {
        return new ImportJobMappingSuggestionPromptContext(
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
                        .map(item -> new ImportJobMappingSuggestionPromptContext.ErrorCodeCount(item.errorCode(), item.count()))
                        .toList(),
                toHeaderSignalContext(headerSignalCandidate),
                toGlobalErrorContexts(detail.itemErrors()),
                promptWindowErrors.stream().map(this::toErrorRowContext).toList()
        );
    }

    private ImportJobMappingSuggestionPromptContext.HeaderSignalContext toHeaderSignalContext(HeaderSignalCandidate candidate) {
        return new ImportJobMappingSuggestionPromptContext.HeaderSignalContext(
                candidate.sourceError().errorCode(),
                candidate.sourceError().errorMessage(),
                candidate.headerSignal().headerColumnCount(),
                candidate.headerSignal().observedColumns().stream()
                        .map(column -> new ImportJobMappingSuggestionPromptContext.ObservedColumn(column.headerName(), column.headerPosition()))
                        .toList()
        );
    }

    private List<ImportJobMappingSuggestionPromptContext.GlobalErrorContext> toGlobalErrorContexts(List<ImportJobErrorRecord> itemErrors) {
        if (itemErrors == null || itemErrors.isEmpty()) {
            return List.of();
        }
        return itemErrors.stream()
                .filter(error -> error != null && error.rowNumber() == null)
                .map(error -> new ImportJobMappingSuggestionPromptContext.GlobalErrorContext(error.errorCode(), error.errorMessage()))
                .toList();
    }

    private ImportJobMappingSuggestionPromptContext.ErrorRowContext toErrorRowContext(ImportJobErrorRecord error) {
        ImportJobAiSanitizationSupport.SanitizedRowSummary rowSummary = ImportJobAiSanitizationSupport.summarizeRowPayload(error.rawPayload());
        return new ImportJobMappingSuggestionPromptContext.ErrorRowContext(
                error.rowNumber(),
                error.errorCode(),
                error.errorMessage(),
                new ImportJobMappingSuggestionPromptContext.SanitizedRowSummary(
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

    private List<ImportJobAiMappingSuggestionResponse.SuggestedFieldMapping> normalizeMappings(
            List<ImportJobMappingSuggestionProviderResult.SuggestedFieldMapping> providerMappings,
            ImportJobAiSanitizationSupport.SanitizedHeaderSignal headerSignal
    ) {
        if (providerMappings == null || providerMappings.isEmpty()) {
            throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider import mapping suggestion payload is missing suggestedFieldMappings");
        }
        Map<String, ImportJobAiMappingSuggestionResponse.SuggestedFieldMapping> normalizedByCanonical = new LinkedHashMap<>();
        for (ImportJobMappingSuggestionProviderResult.SuggestedFieldMapping item : providerMappings) {
            if (item == null) {
                throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider import mapping suggestion payload has invalid suggestedFieldMappings item");
            }
            String canonicalField = normalizeRequiredText(item.canonicalField(), "suggestedFieldMappings.canonicalField");
            if (!ImportCsvSupport.USER_CSV_HEADERS.contains(canonicalField)) {
                throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider import mapping suggestion payload has unsupported canonical field " + canonicalField);
            }
            if (normalizedByCanonical.containsKey(canonicalField)) {
                throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider import mapping suggestion payload has duplicate canonical field " + canonicalField);
            }
            String reasoning = normalizeRequiredText(item.reasoning(), "suggestedFieldMappings.reasoning");
            ImportJobAiMappingSuggestionResponse.ObservedColumnSignal observedColumnSignal =
                    normalizeObservedColumnSignal(item.observedColumnSignal(), headerSignal);
            if (observedColumnSignal == null && !item.reviewRequired()) {
                throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider import mapping suggestion payload requires reviewRequired=true when observedColumnSignal is null");
            }
            normalizedByCanonical.put(canonicalField, new ImportJobAiMappingSuggestionResponse.SuggestedFieldMapping(
                    canonicalField,
                    observedColumnSignal,
                    reasoning,
                    item.reviewRequired()
            ));
        }
        if (normalizedByCanonical.size() != ImportCsvSupport.USER_CSV_HEADERS.size()) {
            throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider import mapping suggestion payload is missing canonical USER_CSV fields");
        }
        return ImportCsvSupport.USER_CSV_HEADERS.stream()
                .map(normalizedByCanonical::get)
                .toList();
    }

    private ImportJobAiMappingSuggestionResponse.ObservedColumnSignal normalizeObservedColumnSignal(
            ImportJobMappingSuggestionProviderResult.ObservedColumnSignal observedColumnSignal,
            ImportJobAiSanitizationSupport.SanitizedHeaderSignal headerSignal
    ) {
        if (observedColumnSignal == null) {
            return null;
        }
        String rawHeaderName = normalizeRequiredText(observedColumnSignal.headerName(), "suggestedFieldMappings.observedColumnSignal.headerName");
        String headerName = ImportJobAiSanitizationSupport.normalizeHeaderToken(rawHeaderName);
        if (!StringUtils.hasText(headerName)) {
            throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider import mapping suggestion payload has invalid suggestedFieldMappings.observedColumnSignal.headerName");
        }
        Integer headerPosition = observedColumnSignal.headerPosition();
        if (headerPosition == null || headerPosition < 1) {
            throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider import mapping suggestion payload has invalid suggestedFieldMappings.observedColumnSignal.headerPosition");
        }
        if (headerSignal.headerColumnCount() != null && headerPosition > headerSignal.headerColumnCount()) {
            throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider import mapping suggestion payload has invalid suggestedFieldMappings.observedColumnSignal.headerPosition");
        }
        // Provider-returned observed signals must be proven against the local sanitized header context before success is returned.
        ImportJobAiSanitizationSupport.ObservedHeaderColumn groundedColumn = headerSignal.observedColumns().stream()
                .filter(column -> column.headerPosition() == headerPosition && headerName.equals(column.headerName()))
                .findFirst()
                .orElseThrow(() -> new AiProviderException(
                        AiProviderFailureType.INVALID_RESPONSE,
                        "provider import mapping suggestion payload has ungrounded suggestedFieldMappings.observedColumnSignal"
                ));
        return new ImportJobAiMappingSuggestionResponse.ObservedColumnSignal(groundedColumn.headerName(), groundedColumn.headerPosition());
    }

    private String normalizeRequiredText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider import mapping suggestion payload is missing " + fieldName);
        }
        return value.trim();
    }

    private List<String> normalizeRequiredItems(List<String> items, String fieldName) {
        if (items == null || items.isEmpty()) {
            throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider import mapping suggestion payload is missing " + fieldName);
        }
        List<String> normalized = items.stream()
                .map(item -> item == null ? null : item.trim())
                .filter(StringUtils::hasText)
                .toList();
        if (normalized.size() != items.size()) {
            throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider import mapping suggestion payload has blank " + fieldName + " item");
        }
        return normalized;
    }

    private record HeaderSignalCandidate(
            ImportJobErrorRecord sourceError,
            ImportJobAiSanitizationSupport.SanitizedHeaderSignal headerSignal
    ) {
    }
}
