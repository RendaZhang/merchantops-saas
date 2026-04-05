package com.renda.merchantops.api.importjob.ai;

import com.renda.merchantops.api.ai.core.AiGenerationWorkflow;
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
import com.renda.merchantops.domain.importjob.ImportJobDetail;
import com.renda.merchantops.domain.importjob.ImportJobErrorPageCriteria;
import com.renda.merchantops.domain.importjob.ImportJobErrorRecord;
import com.renda.merchantops.domain.importjob.ImportJobQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ImportJobAiErrorSummaryService {

    private static final AiGenerationWorkflow WORKFLOW = AiGenerationWorkflow.IMPORT_ERROR_SUMMARY;
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
        String promptVersion = WORKFLOW.resolvePromptVersion(aiProperties, aiInteractionExecutionSupport);
        String configuredModelId = aiInteractionExecutionSupport.normalizeNullable(aiProperties.resolveModelId());
        ImportJobErrorSummaryPromptContext promptContext = toPromptContext(detail, promptWindowErrors);
        ImportJobErrorSummaryPrompt prompt = importJobErrorSummaryPromptBuilder.build(promptVersion, promptContext);

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
                    WORKFLOW.entityType(),
                    importJobId,
                    WORKFLOW.interactionType(),
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
                    WORKFLOW.entityType(),
                    importJobId,
                    WORKFLOW.interactionType(),
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
        ImportJobAiSanitizationSupport.SanitizedRowSummary rowSummary = ImportJobAiSanitizationSupport.summarizeRowPayload(error.rawPayload());
        return new ImportJobErrorSummaryPromptContext.ErrorRowContext(
                error.rowNumber(),
                error.errorCode(),
                error.errorMessage(),
                new ImportJobErrorSummaryPromptContext.SanitizedRowSummary(
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
