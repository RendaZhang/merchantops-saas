package com.renda.merchantops.api.importjob.ai;

import com.renda.merchantops.api.ai.core.AiInteractionExecutionSupport;
import com.renda.merchantops.api.ai.importjob.errorsummary.ImportJobErrorSummaryAiProvider;
import com.renda.merchantops.api.ai.importjob.errorsummary.ImportJobErrorSummaryPromptBuilder;
import com.renda.merchantops.api.ai.importjob.errorsummary.ImportJobErrorSummaryProviderRequest;
import com.renda.merchantops.api.ai.importjob.errorsummary.ImportJobErrorSummaryProviderResult;
import com.renda.merchantops.api.config.AiProperties;
import com.renda.merchantops.api.config.AiProviderType;
import com.renda.merchantops.domain.ai.AiInteractionRecordCommand;
import com.renda.merchantops.domain.ai.AiInteractionRecordUseCase;
import com.renda.merchantops.domain.ai.AiInteractionStatus;
import com.renda.merchantops.domain.importjob.ImportJobDetail;
import com.renda.merchantops.domain.importjob.ImportJobErrorCount;
import com.renda.merchantops.domain.importjob.ImportJobErrorPageCriteria;
import com.renda.merchantops.domain.importjob.ImportJobErrorPageResult;
import com.renda.merchantops.domain.importjob.ImportJobErrorRecord;
import com.renda.merchantops.domain.importjob.ImportJobQueryUseCase;
import com.renda.merchantops.domain.importjob.ImportJobRecord;
import com.renda.merchantops.domain.shared.error.BizException;
import com.renda.merchantops.domain.shared.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImportJobAiErrorSummaryServiceTest {

    private final ImportJobQueryUseCase importJobQueryUseCase = mock(ImportJobQueryUseCase.class);
    private final ImportJobErrorSummaryAiProvider importJobErrorSummaryAiProvider = mock(ImportJobErrorSummaryAiProvider.class);
    private final AiInteractionRecordUseCase recordUseCase = mock(AiInteractionRecordUseCase.class);

    @Test
    void generateErrorSummaryShouldUseFallbackPromptVersionAndSanitizePromptContext() {
        AiProperties aiProperties = aiProperties();
        aiProperties.setImportErrorSummaryPromptVersion("   ");
        when(importJobQueryUseCase.getJobDetail(1L, 7001L)).thenReturn(sampleDetail());
        when(importJobQueryUseCase.pageJobErrors(eq(1L), eq(7001L), eq(new ImportJobErrorPageCriteria(0, 20, null))))
                .thenReturn(new ImportJobErrorPageResult(List.of(sampleError()), 0, 20, 1, 1));
        when(importJobErrorSummaryAiProvider.generateErrorSummary(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new ImportJobErrorSummaryProviderResult(
                        "Role validation dominates the job.",
                        List.of("UNKNOWN_ROLE appears across structurally complete rows."),
                        List.of("Correct tenant role mappings before replay."),
                        "gpt-4.1-mini",
                        120,
                        40,
                        160,
                        null
                ));

        ImportJobAiErrorSummaryService service = new ImportJobAiErrorSummaryService(
                importJobQueryUseCase,
                new ImportJobErrorSummaryPromptBuilder(),
                importJobErrorSummaryAiProvider,
                new AiInteractionExecutionSupport(recordUseCase),
                com.renda.merchantops.api.support.TestFeatureFlagGateSupport.alwaysEnabledGateService(),
                aiProperties
        );

        var response = service.generateErrorSummary(1L, 101L, "import-ai-error-summary-req-1", 7001L);

        assertThat(response.importJobId()).isEqualTo(7001L);
        assertThat(response.promptVersion()).isEqualTo("import-error-summary-v1");
        assertThat(response.topErrorPatterns()).containsExactly("UNKNOWN_ROLE appears across structurally complete rows.");
        assertThat(response.recommendedNextSteps()).containsExactly("Correct tenant role mappings before replay.");

        ArgumentCaptor<ImportJobErrorSummaryProviderRequest> requestCaptor = ArgumentCaptor.forClass(ImportJobErrorSummaryProviderRequest.class);
        verify(importJobErrorSummaryAiProvider).generateErrorSummary(requestCaptor.capture());
        String userPrompt = requestCaptor.getValue().prompt().userPrompt();
        assertThat(userPrompt).contains("passwordPresent=true");
        assertThat(userPrompt).contains("roleCodeCount=1");
        assertThat(userPrompt).doesNotContain("retry-user");
        assertThat(userPrompt).doesNotContain("retry-user@example.com");
        assertThat(userPrompt).doesNotContain("abc123");
        assertThat(userPrompt).doesNotContain("retry-user,Retry User,retry-user@example.com,abc123,READ_ONLY");

        ArgumentCaptor<AiInteractionRecordCommand> commandCaptor = ArgumentCaptor.forClass(AiInteractionRecordCommand.class);
        verify(recordUseCase).record(commandCaptor.capture());
        assertThat(commandCaptor.getValue().entityType()).isEqualTo("IMPORT_JOB");
        assertThat(commandCaptor.getValue().interactionType()).isEqualTo("ERROR_SUMMARY");
        assertThat(commandCaptor.getValue().promptVersion()).isEqualTo("import-error-summary-v1");
        assertThat(commandCaptor.getValue().status()).isEqualTo(AiInteractionStatus.SUCCEEDED);
        assertThat(commandCaptor.getValue().outputSummary()).isEqualTo("Role validation dominates the job.");
    }

    @Test
    void generateErrorSummaryShouldRejectBlankArrayItemsFromProviderResult() {
        when(importJobQueryUseCase.getJobDetail(1L, 7001L)).thenReturn(sampleDetail());
        when(importJobQueryUseCase.pageJobErrors(eq(1L), eq(7001L), eq(new ImportJobErrorPageCriteria(0, 20, null))))
                .thenReturn(new ImportJobErrorPageResult(List.of(sampleError()), 0, 20, 1, 1));
        when(importJobErrorSummaryAiProvider.generateErrorSummary(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new ImportJobErrorSummaryProviderResult(
                        "Role validation dominates the job.",
                        List.of("pattern one", "   "),
                        List.of("step one"),
                        "gpt-4.1-mini",
                        120,
                        40,
                        160,
                        null
                ));

        ImportJobAiErrorSummaryService service = new ImportJobAiErrorSummaryService(
                importJobQueryUseCase,
                new ImportJobErrorSummaryPromptBuilder(),
                importJobErrorSummaryAiProvider,
                new AiInteractionExecutionSupport(recordUseCase),
                com.renda.merchantops.api.support.TestFeatureFlagGateSupport.alwaysEnabledGateService(),
                aiProperties()
        );

        assertThatThrownBy(() -> service.generateErrorSummary(1L, 101L, "import-ai-error-summary-req-2", 7001L))
                .isInstanceOfSatisfying(BizException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SERVICE_UNAVAILABLE);
                    assertThat(ex.getMessage()).isEqualTo("import ai error summary is unavailable");
                });

        ArgumentCaptor<AiInteractionRecordCommand> commandCaptor = ArgumentCaptor.forClass(AiInteractionRecordCommand.class);
        verify(recordUseCase).record(commandCaptor.capture());
        assertThat(commandCaptor.getValue().status()).isEqualTo(AiInteractionStatus.INVALID_RESPONSE);
    }

    private AiProperties aiProperties() {
        AiProperties aiProperties = new AiProperties();
        aiProperties.setEnabled(true);
        aiProperties.setProvider(AiProviderType.OPENAI);
        aiProperties.setImportErrorSummaryPromptVersion("import-error-summary-v1");
        aiProperties.setModelId("gpt-4.1-mini");
        aiProperties.setTimeoutMs(1000);
        aiProperties.setApiKey("test-key");
        aiProperties.setBaseUrl("https://api.openai.com");
        return aiProperties;
    }

    private ImportJobDetail sampleDetail() {
        ImportJobRecord job = new ImportJobRecord(
                7001L,
                1L,
                "USER_CSV",
                "CSV",
                "source-authz.csv",
                "1/source-authz.csv",
                null,
                "FAILED",
                101L,
                "req-source-authz",
                2,
                0,
                2,
                "all rows failed validation",
                LocalDateTime.of(2026, 3, 27, 9, 0, 0),
                LocalDateTime.of(2026, 3, 27, 9, 0, 2),
                LocalDateTime.of(2026, 3, 27, 9, 0, 5)
        );
        return new ImportJobDetail(job, List.of(new ImportJobErrorCount("UNKNOWN_ROLE", 2L)), List.of());
    }

    private ImportJobErrorRecord sampleError() {
        return new ImportJobErrorRecord(
                7101L,
                1L,
                7001L,
                2,
                "UNKNOWN_ROLE",
                "roleCodes must exist in current tenant",
                "retry-user,Retry User,retry-user@example.com,abc123,READ_ONLY",
                LocalDateTime.of(2026, 3, 27, 9, 0, 4)
        );
    }
}
