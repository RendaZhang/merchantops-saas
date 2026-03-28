package com.renda.merchantops.api.importjob.ai;

import com.renda.merchantops.api.ai.core.AiInteractionExecutionSupport;
import com.renda.merchantops.api.ai.importjob.fixrecommendation.ImportJobFixRecommendationAiProvider;
import com.renda.merchantops.api.ai.importjob.fixrecommendation.ImportJobFixRecommendationPromptBuilder;
import com.renda.merchantops.api.ai.importjob.fixrecommendation.ImportJobFixRecommendationProviderRequest;
import com.renda.merchantops.api.ai.importjob.fixrecommendation.ImportJobFixRecommendationProviderResult;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ImportJobAiFixRecommendationServiceTest {

    private final ImportJobQueryUseCase importJobQueryUseCase = mock(ImportJobQueryUseCase.class);
    private final ImportJobFixRecommendationAiProvider importJobFixRecommendationAiProvider = mock(ImportJobFixRecommendationAiProvider.class);
    private final AiInteractionRecordUseCase recordUseCase = mock(AiInteractionRecordUseCase.class);

    @Test
    void generateFixRecommendationShouldUseFallbackPromptVersionAndSanitizePromptContext() {
        AiProperties aiProperties = aiProperties();
        aiProperties.setImportFixRecommendationPromptVersion("   ");
        when(importJobQueryUseCase.getJobDetail(1L, 7001L)).thenReturn(sampleDetailWithRowSignals());
        when(importJobQueryUseCase.pageJobErrors(eq(1L), eq(7001L), eq(new ImportJobErrorPageCriteria(0, 20, null))))
                .thenReturn(new ImportJobErrorPageResult(List.of(sampleUnknownRoleError(), sampleDuplicateUsernameError()), 0, 20, 2, 1));
        when(importJobFixRecommendationAiProvider.generateFixRecommendation(any())).thenReturn(sampleProviderResult());

        ImportJobAiFixRecommendationService service = new ImportJobAiFixRecommendationService(
                importJobQueryUseCase,
                new ImportJobFixRecommendationPromptBuilder(),
                importJobFixRecommendationAiProvider,
                new AiInteractionExecutionSupport(recordUseCase),
                aiProperties
        );

        var response = service.generateFixRecommendation(1L, 101L, "import-ai-fix-recommendation-req-1", 7001L);

        assertThat(response.importJobId()).isEqualTo(7001L);
        assertThat(response.summary()).contains("tenant role validation");
        assertThat(response.promptVersion()).isEqualTo("import-fix-recommendation-v1");
        assertThat(response.recommendedFixes()).hasSize(2);
        assertThat(response.recommendedFixes())
                .extracting(item -> item.errorCode())
                .containsExactly("UNKNOWN_ROLE", "DUPLICATE_USERNAME");
        assertThat(response.recommendedFixes().get(0).affectedRowsEstimate()).isEqualTo(7L);
        assertThat(response.recommendedFixes().get(1).affectedRowsEstimate()).isEqualTo(2L);

        ArgumentCaptor<ImportJobFixRecommendationProviderRequest> requestCaptor =
                ArgumentCaptor.forClass(ImportJobFixRecommendationProviderRequest.class);
        verify(importJobFixRecommendationAiProvider).generateFixRecommendation(requestCaptor.capture());
        String userPrompt = requestCaptor.getValue().prompt().userPrompt();
        assertThat(userPrompt).contains("Grounded row-level failure groups");
        assertThat(userPrompt).contains("errorCode: UNKNOWN_ROLE");
        assertThat(userPrompt).contains("affectedRowsEstimate: 7");
        assertThat(userPrompt).contains("errorCode: DUPLICATE_USERNAME");
        assertThat(userPrompt).contains("passwordPresent=true");
        assertThat(userPrompt).doesNotContain("retry-user@example.com");
        assertThat(userPrompt).doesNotContain("secret-pass-1");
        assertThat(userPrompt).doesNotContain("retry-user,Retry User,retry-user@example.com,secret-pass-1,READ_ONLY");
        assertThat(userPrompt).doesNotContain("existing-user,Existing User,existing@example.com,password-2,READ_ONLY");
        assertThat(userPrompt).doesNotContain("READ_ONLY");

        ArgumentCaptor<AiInteractionRecordCommand> commandCaptor = ArgumentCaptor.forClass(AiInteractionRecordCommand.class);
        verify(recordUseCase).record(commandCaptor.capture());
        assertThat(commandCaptor.getValue().entityType()).isEqualTo("IMPORT_JOB");
        assertThat(commandCaptor.getValue().interactionType()).isEqualTo("FIX_RECOMMENDATION");
        assertThat(commandCaptor.getValue().promptVersion()).isEqualTo("import-fix-recommendation-v1");
        assertThat(commandCaptor.getValue().status()).isEqualTo(AiInteractionStatus.SUCCEEDED);
        assertThat(commandCaptor.getValue().outputSummary()).isEqualTo(sampleProviderResult().summary());
    }

    @Test
    void generateFixRecommendationShouldRejectJobWithoutFailureSignals() {
        when(importJobQueryUseCase.getJobDetail(1L, 7003L)).thenReturn(sampleDetailWithoutFailureSignals());

        ImportJobAiFixRecommendationService service = new ImportJobAiFixRecommendationService(
                importJobQueryUseCase,
                new ImportJobFixRecommendationPromptBuilder(),
                importJobFixRecommendationAiProvider,
                new AiInteractionExecutionSupport(recordUseCase),
                aiProperties()
        );

        assertThatThrownBy(() -> service.generateFixRecommendation(1L, 101L, "import-ai-fix-recommendation-no-failure-1", 7003L))
                .isInstanceOfSatisfying(BizException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
                    assertThat(ex.getMessage()).isEqualTo("import job has no failure signals for fix recommendation");
                });

        verifyNoInteractions(importJobFixRecommendationAiProvider);
        verifyNoInteractions(recordUseCase);
    }

    @Test
    void generateFixRecommendationShouldRejectUnsupportedImportType() {
        when(importJobQueryUseCase.getJobDetail(1L, 7005L)).thenReturn(sampleUnsupportedImportTypeDetail());

        ImportJobAiFixRecommendationService service = new ImportJobAiFixRecommendationService(
                importJobQueryUseCase,
                new ImportJobFixRecommendationPromptBuilder(),
                importJobFixRecommendationAiProvider,
                new AiInteractionExecutionSupport(recordUseCase),
                aiProperties()
        );

        assertThatThrownBy(() -> service.generateFixRecommendation(1L, 101L, "import-ai-fix-recommendation-unsupported-1", 7005L))
                .isInstanceOfSatisfying(BizException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
                    assertThat(ex.getMessage()).isEqualTo("import ai fix recommendation only supports USER_CSV");
                });

        verifyNoInteractions(importJobFixRecommendationAiProvider);
        verifyNoInteractions(recordUseCase);
    }

    @Test
    void generateFixRecommendationShouldRejectJobWithoutRowSignals() {
        when(importJobQueryUseCase.getJobDetail(1L, 7004L)).thenReturn(sampleDetailWithoutRowSignals());
        when(importJobQueryUseCase.pageJobErrors(eq(1L), eq(7004L), eq(new ImportJobErrorPageCriteria(0, 20, null))))
                .thenReturn(new ImportJobErrorPageResult(List.of(sampleHeaderOnlyError()), 0, 20, 1, 1));

        ImportJobAiFixRecommendationService service = new ImportJobAiFixRecommendationService(
                importJobQueryUseCase,
                new ImportJobFixRecommendationPromptBuilder(),
                importJobFixRecommendationAiProvider,
                new AiInteractionExecutionSupport(recordUseCase),
                aiProperties()
        );

        assertThatThrownBy(() -> service.generateFixRecommendation(1L, 101L, "import-ai-fix-recommendation-no-row-1", 7004L))
                .isInstanceOfSatisfying(BizException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
                    assertThat(ex.getMessage()).isEqualTo("import job has no sanitized row signal for fix recommendation");
                });

        verifyNoInteractions(importJobFixRecommendationAiProvider);
        verifyNoInteractions(recordUseCase);
    }

    @Test
    void generateFixRecommendationShouldPersistInvalidResponseWhenProviderUsesUnknownErrorCode() {
        when(importJobQueryUseCase.getJobDetail(1L, 7001L)).thenReturn(sampleDetailWithRowSignals());
        when(importJobQueryUseCase.pageJobErrors(eq(1L), eq(7001L), eq(new ImportJobErrorPageCriteria(0, 20, null))))
                .thenReturn(new ImportJobErrorPageResult(List.of(sampleUnknownRoleError(), sampleDuplicateUsernameError()), 0, 20, 2, 1));
        when(importJobFixRecommendationAiProvider.generateFixRecommendation(any()))
                .thenReturn(new ImportJobFixRecommendationProviderResult(
                        "The job still needs a role cleanup.",
                        List.of(new ImportJobFixRecommendationProviderResult.RecommendedFix(
                                "INVALID_EMAIL",
                                "Review the email values before replay.",
                                "The data looks malformed.",
                                true
                        )),
                        List.of("Still review tenant rules."),
                        List.of("Check /errors before replay."),
                        "gpt-4.1-mini",
                        120,
                        44,
                        164,
                        null
                ));

        ImportJobAiFixRecommendationService service = new ImportJobAiFixRecommendationService(
                importJobQueryUseCase,
                new ImportJobFixRecommendationPromptBuilder(),
                importJobFixRecommendationAiProvider,
                new AiInteractionExecutionSupport(recordUseCase),
                aiProperties()
        );

        assertThatThrownBy(() -> service.generateFixRecommendation(1L, 101L, "import-ai-fix-recommendation-invalid-response-1", 7001L))
                .isInstanceOfSatisfying(BizException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SERVICE_UNAVAILABLE);
                    assertThat(ex.getMessage()).isEqualTo("import ai fix recommendation is unavailable");
                });

        ArgumentCaptor<AiInteractionRecordCommand> commandCaptor = ArgumentCaptor.forClass(AiInteractionRecordCommand.class);
        verify(recordUseCase).record(commandCaptor.capture());
        assertThat(commandCaptor.getValue().status()).isEqualTo(AiInteractionStatus.INVALID_RESPONSE);
        assertThat(commandCaptor.getValue().interactionType()).isEqualTo("FIX_RECOMMENDATION");
    }

    @Test
    void generateFixRecommendationShouldPersistInvalidResponseWhenProviderEchoesSensitiveValue() {
        when(importJobQueryUseCase.getJobDetail(1L, 7001L)).thenReturn(sampleDetailWithRowSignals());
        when(importJobQueryUseCase.pageJobErrors(eq(1L), eq(7001L), eq(new ImportJobErrorPageCriteria(0, 20, null))))
                .thenReturn(new ImportJobErrorPageResult(List.of(sampleUnknownRoleError(), sampleDuplicateUsernameError()), 0, 20, 2, 1));
        when(importJobFixRecommendationAiProvider.generateFixRecommendation(any()))
                .thenReturn(new ImportJobFixRecommendationProviderResult(
                        "The job is blocked by UNKNOWN_ROLE.",
                        List.of(new ImportJobFixRecommendationProviderResult.RecommendedFix(
                                "UNKNOWN_ROLE",
                                "Replace the invalid role with READ_ONLY before replay.",
                                "The sampled rows still reference READ_ONLY incorrectly.",
                                true
                        )),
                        List.of("Still review tenant rules."),
                        List.of("Check /errors before replay."),
                        "gpt-4.1-mini",
                        120,
                        44,
                        164,
                        null
                ));

        ImportJobAiFixRecommendationService service = new ImportJobAiFixRecommendationService(
                importJobQueryUseCase,
                new ImportJobFixRecommendationPromptBuilder(),
                importJobFixRecommendationAiProvider,
                new AiInteractionExecutionSupport(recordUseCase),
                aiProperties()
        );

        assertThatThrownBy(() -> service.generateFixRecommendation(1L, 101L, "import-ai-fix-recommendation-sensitive-echo-1", 7001L))
                .isInstanceOfSatisfying(BizException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SERVICE_UNAVAILABLE);
                    assertThat(ex.getMessage()).isEqualTo("import ai fix recommendation is unavailable");
                });

        ArgumentCaptor<AiInteractionRecordCommand> commandCaptor = ArgumentCaptor.forClass(AiInteractionRecordCommand.class);
        verify(recordUseCase).record(commandCaptor.capture());
        assertThat(commandCaptor.getValue().status()).isEqualTo(AiInteractionStatus.INVALID_RESPONSE);
        assertThat(commandCaptor.getValue().interactionType()).isEqualTo("FIX_RECOMMENDATION");
    }

    private AiProperties aiProperties() {
        AiProperties aiProperties = new AiProperties();
        aiProperties.setEnabled(true);
        aiProperties.setProvider(AiProviderType.OPENAI);
        aiProperties.setImportFixRecommendationPromptVersion("import-fix-recommendation-v1");
        aiProperties.setModelId("gpt-4.1-mini");
        aiProperties.setTimeoutMs(1000);
        aiProperties.setApiKey("test-key");
        aiProperties.setBaseUrl("https://api.openai.com");
        return aiProperties;
    }

    private ImportJobDetail sampleDetailWithRowSignals() {
        ImportJobRecord job = new ImportJobRecord(
                7001L,
                1L,
                "USER_CSV",
                "CSV",
                "row-errors.csv",
                "1/row-errors.csv",
                null,
                "FAILED",
                101L,
                "req-row-errors",
                9,
                0,
                9,
                "role validation and duplicate usernames blocked the import",
                LocalDateTime.of(2026, 3, 28, 9, 0, 0),
                LocalDateTime.of(2026, 3, 28, 9, 0, 2),
                LocalDateTime.of(2026, 3, 28, 9, 0, 5)
        );
        return new ImportJobDetail(
                job,
                List.of(
                        new ImportJobErrorCount("UNKNOWN_ROLE", 7L),
                        new ImportJobErrorCount("DUPLICATE_USERNAME", 2L)
                ),
                List.of(sampleUnknownRoleError(), sampleDuplicateUsernameError())
        );
    }

    private ImportJobDetail sampleDetailWithoutFailureSignals() {
        ImportJobRecord job = new ImportJobRecord(
                7003L,
                1L,
                "USER_CSV",
                "CSV",
                "clean.csv",
                "1/clean.csv",
                null,
                "SUCCEEDED",
                101L,
                "req-clean",
                2,
                2,
                0,
                null,
                LocalDateTime.of(2026, 3, 28, 9, 10, 0),
                LocalDateTime.of(2026, 3, 28, 9, 10, 2),
                LocalDateTime.of(2026, 3, 28, 9, 10, 5)
        );
        return new ImportJobDetail(job, List.of(), List.of());
    }

    private ImportJobDetail sampleDetailWithoutRowSignals() {
        ImportJobRecord job = new ImportJobRecord(
                7004L,
                1L,
                "USER_CSV",
                "CSV",
                "header-only.csv",
                "1/header-only.csv",
                null,
                "FAILED",
                101L,
                "req-header-only",
                1,
                0,
                1,
                "header validation failed before row processing",
                LocalDateTime.of(2026, 3, 28, 9, 20, 0),
                LocalDateTime.of(2026, 3, 28, 9, 20, 2),
                LocalDateTime.of(2026, 3, 28, 9, 20, 5)
        );
        return new ImportJobDetail(job, List.of(new ImportJobErrorCount("INVALID_HEADER", 1L)), List.of(sampleHeaderOnlyError()));
    }

    private ImportJobDetail sampleUnsupportedImportTypeDetail() {
        ImportJobRecord job = new ImportJobRecord(
                7005L,
                1L,
                "ORDER_CSV",
                "CSV",
                "orders.csv",
                "1/orders.csv",
                null,
                "FAILED",
                101L,
                "req-orders",
                1,
                0,
                1,
                "unsupported import type for fix recommendation",
                LocalDateTime.of(2026, 3, 28, 9, 30, 0),
                LocalDateTime.of(2026, 3, 28, 9, 30, 2),
                LocalDateTime.of(2026, 3, 28, 9, 30, 5)
        );
        return new ImportJobDetail(job, List.of(new ImportJobErrorCount("UNKNOWN_ROLE", 1L)), List.of(sampleUnknownRoleError()));
    }

    private ImportJobErrorRecord sampleUnknownRoleError() {
        return new ImportJobErrorRecord(
                7101L,
                1L,
                7001L,
                2,
                "UNKNOWN_ROLE",
                "roleCodes must exist in current tenant",
                "retry-user,Retry User,retry-user@example.com,secret-pass-1,READ_ONLY",
                LocalDateTime.of(2026, 3, 28, 9, 0, 3)
        );
    }

    private ImportJobErrorRecord sampleDuplicateUsernameError() {
        return new ImportJobErrorRecord(
                7102L,
                1L,
                7001L,
                3,
                "DUPLICATE_USERNAME",
                "username already exists in current tenant",
                "existing-user,Existing User,existing@example.com,password-2,READ_ONLY",
                LocalDateTime.of(2026, 3, 28, 9, 0, 4)
        );
    }

    private ImportJobErrorRecord sampleHeaderOnlyError() {
        return new ImportJobErrorRecord(
                7201L,
                1L,
                7004L,
                null,
                "INVALID_HEADER",
                "header columns do not match USER_CSV",
                "login,display_name,email_address,passwd,roles",
                LocalDateTime.of(2026, 3, 28, 9, 20, 3)
        );
    }

    private ImportJobFixRecommendationProviderResult sampleProviderResult() {
        return new ImportJobFixRecommendationProviderResult(
                "The job is mostly blocked by tenant role validation, with a smaller duplicate-username tail that should be handled separately.",
                List.of(
                        new ImportJobFixRecommendationProviderResult.RecommendedFix(
                                "UNKNOWN_ROLE",
                                "Verify that the referenced role codes exist in the current tenant before preparing replay input.",
                                "The sampled failures point to tenant role validation rather than CSV shape corruption.",
                                true
                        ),
                        new ImportJobFixRecommendationProviderResult.RecommendedFix(
                                "DUPLICATE_USERNAME",
                                "Review the source usernames against current-tenant users before replay.",
                                "The sampled failures indicate a uniqueness conflict that needs operator review.",
                                true
                        )
                ),
                List.of("The recommendations are grounded in row-level error groups and still require operator review."),
                List.of("Review the affected rows in /errors before editing replay input."),
                "gpt-4.1-mini",
                150,
                52,
                202,
                null
        );
    }
}
