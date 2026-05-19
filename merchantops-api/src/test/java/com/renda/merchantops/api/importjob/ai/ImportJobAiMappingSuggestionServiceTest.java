package com.renda.merchantops.api.importjob.ai;

import com.renda.merchantops.api.ai.core.AiInteractionExecutionSupport;
import com.renda.merchantops.api.ai.importjob.mappingsuggestion.ImportJobMappingSuggestionAiProvider;
import com.renda.merchantops.api.ai.importjob.mappingsuggestion.ImportJobMappingSuggestionPromptBuilder;
import com.renda.merchantops.api.ai.importjob.mappingsuggestion.ImportJobMappingSuggestionProviderRequest;
import com.renda.merchantops.api.ai.importjob.mappingsuggestion.ImportJobMappingSuggestionProviderResult;
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

class ImportJobAiMappingSuggestionServiceTest {

    private final ImportJobQueryUseCase importJobQueryUseCase = mock(ImportJobQueryUseCase.class);
    private final ImportJobMappingSuggestionAiProvider importJobMappingSuggestionAiProvider = mock(ImportJobMappingSuggestionAiProvider.class);
    private final AiInteractionRecordUseCase recordUseCase = mock(AiInteractionRecordUseCase.class);

    @Test
    void generateMappingSuggestionShouldUseFallbackPromptVersionAndSanitizePromptContext() {
        AiProperties aiProperties = aiProperties();
        aiProperties.setImportMappingSuggestionPromptVersion("   ");
        when(importJobQueryUseCase.getJobDetail(1L, 7002L)).thenReturn(sampleDetailWithHeaderSignal());
        when(importJobQueryUseCase.pageJobErrors(eq(1L), eq(7002L), eq(new ImportJobErrorPageCriteria(0, 20, null))))
                .thenReturn(new ImportJobErrorPageResult(List.of(sampleRowError()), 0, 20, 1, 1));
        when(importJobMappingSuggestionAiProvider.generateMappingSuggestion(any())).thenReturn(sampleProviderResult());

        ImportJobAiMappingSuggestionService service = new ImportJobAiMappingSuggestionService(
                importJobQueryUseCase,
                new ImportJobMappingSuggestionPromptBuilder(),
                importJobMappingSuggestionAiProvider,
                new AiInteractionExecutionSupport(recordUseCase),
                com.renda.merchantops.api.support.TestFeatureFlagGateSupport.alwaysEnabledGateService(),
                aiProperties
        );

        var response = service.generateMappingSuggestion(1L, 101L, "import-ai-mapping-suggestion-req-1", 7002L);

        assertThat(response.importJobId()).isEqualTo(7002L);
        assertThat(response.summary()).contains("failed header");
        assertThat(response.promptVersion()).isEqualTo("import-mapping-suggestion-v1");
        assertThat(response.suggestedFieldMappings()).hasSize(5);
        assertThat(response.suggestedFieldMappings())
                .extracting(item -> item.canonicalField())
                .containsExactly("username", "displayName", "email", "password", "roleCodes");
        assertThat(response.suggestedFieldMappings().get(3).reviewRequired()).isTrue();
        assertThat(response.confidenceNotes()).containsExactly("The source file failed header validation, so each suggested mapping should be reviewed before reuse.");
        assertThat(response.recommendedOperatorChecks()).containsExactly(
                "Confirm the source header order before editing any replay input.",
                "Verify that the observed `roles` column really contains tenant role codes in the expected delimiter format."
        );

        ArgumentCaptor<ImportJobMappingSuggestionProviderRequest> requestCaptor =
                ArgumentCaptor.forClass(ImportJobMappingSuggestionProviderRequest.class);
        verify(importJobMappingSuggestionAiProvider).generateMappingSuggestion(requestCaptor.capture());
        String userPrompt = requestCaptor.getValue().prompt().userPrompt();
        assertThat(userPrompt).contains("sourceErrorCode: INVALID_HEADER");
        assertThat(userPrompt).contains("headerPosition: 1; headerName: login");
        assertThat(userPrompt).contains("headerPosition: 5; headerName: roles");
        assertThat(userPrompt).contains("passwordPresent=true");
        assertThat(userPrompt).contains("roleCodeCount=1");
        assertThat(userPrompt).doesNotContain("login,display_name,email_address,passwd,roles");
        assertThat(userPrompt).doesNotContain("retry-user");
        assertThat(userPrompt).doesNotContain("retry-user@example.com");
        assertThat(userPrompt).doesNotContain("secret-pass-1");
        assertThat(userPrompt).doesNotContain("retry-user,Retry User,retry-user@example.com,secret-pass-1,READ_ONLY");

        ArgumentCaptor<AiInteractionRecordCommand> commandCaptor = ArgumentCaptor.forClass(AiInteractionRecordCommand.class);
        verify(recordUseCase).record(commandCaptor.capture());
        assertThat(commandCaptor.getValue().entityType()).isEqualTo("IMPORT_JOB");
        assertThat(commandCaptor.getValue().interactionType()).isEqualTo("MAPPING_SUGGESTION");
        assertThat(commandCaptor.getValue().promptVersion()).isEqualTo("import-mapping-suggestion-v1");
        assertThat(commandCaptor.getValue().status()).isEqualTo(AiInteractionStatus.SUCCEEDED);
        assertThat(commandCaptor.getValue().outputSummary()).isEqualTo(sampleProviderResult().summary());
    }

    @Test
    void generateMappingSuggestionShouldRejectJobWithoutFailureSignals() {
        when(importJobQueryUseCase.getJobDetail(1L, 7003L)).thenReturn(sampleDetailWithoutFailureSignals());

        ImportJobAiMappingSuggestionService service = new ImportJobAiMappingSuggestionService(
                importJobQueryUseCase,
                new ImportJobMappingSuggestionPromptBuilder(),
                importJobMappingSuggestionAiProvider,
                new AiInteractionExecutionSupport(recordUseCase),
                com.renda.merchantops.api.support.TestFeatureFlagGateSupport.alwaysEnabledGateService(),
                aiProperties()
        );

        assertThatThrownBy(() -> service.generateMappingSuggestion(1L, 101L, "import-ai-mapping-suggestion-no-failure-1", 7003L))
                .isInstanceOfSatisfying(BizException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
                    assertThat(ex.getMessage()).isEqualTo("import job has no failure signals for mapping suggestion");
                });

        verifyNoInteractions(importJobMappingSuggestionAiProvider);
        verifyNoInteractions(recordUseCase);
    }

    @Test
    void generateMappingSuggestionShouldRejectJobWithoutSanitizedHeaderSignal() {
        when(importJobQueryUseCase.getJobDetail(1L, 7001L)).thenReturn(sampleDetailWithoutHeaderSignal());

        ImportJobAiMappingSuggestionService service = new ImportJobAiMappingSuggestionService(
                importJobQueryUseCase,
                new ImportJobMappingSuggestionPromptBuilder(),
                importJobMappingSuggestionAiProvider,
                new AiInteractionExecutionSupport(recordUseCase),
                com.renda.merchantops.api.support.TestFeatureFlagGateSupport.alwaysEnabledGateService(),
                aiProperties()
        );

        assertThatThrownBy(() -> service.generateMappingSuggestion(1L, 101L, "import-ai-mapping-suggestion-no-header-1", 7001L))
                .isInstanceOfSatisfying(BizException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
                    assertThat(ex.getMessage()).isEqualTo("import job has no sanitized header signal for mapping suggestion");
                });

        verifyNoInteractions(importJobMappingSuggestionAiProvider);
        verifyNoInteractions(recordUseCase);
    }

    @Test
    void generateMappingSuggestionShouldPersistInvalidResponseWhenCanonicalMappingsAreIncomplete() {
        when(importJobQueryUseCase.getJobDetail(1L, 7002L)).thenReturn(sampleDetailWithHeaderSignal());
        when(importJobQueryUseCase.pageJobErrors(eq(1L), eq(7002L), eq(new ImportJobErrorPageCriteria(0, 20, null))))
                .thenReturn(new ImportJobErrorPageResult(List.of(sampleRowError()), 0, 20, 1, 1));
        when(importJobMappingSuggestionAiProvider.generateMappingSuggestion(any()))
                .thenReturn(providerResult(List.of(
                        new ImportJobMappingSuggestionProviderResult.SuggestedFieldMapping(
                                "username",
                                new ImportJobMappingSuggestionProviderResult.ObservedColumnSignal("login", 1),
                                "username match",
                                false
                        ),
                        new ImportJobMappingSuggestionProviderResult.SuggestedFieldMapping(
                                "displayName",
                                new ImportJobMappingSuggestionProviderResult.ObservedColumnSignal("display_name", 2),
                                "displayName match",
                                false
                        ),
                        new ImportJobMappingSuggestionProviderResult.SuggestedFieldMapping(
                                "email",
                                new ImportJobMappingSuggestionProviderResult.ObservedColumnSignal("email_address", 3),
                                "email match",
                                false
                        ),
                        new ImportJobMappingSuggestionProviderResult.SuggestedFieldMapping(
                                "password",
                                new ImportJobMappingSuggestionProviderResult.ObservedColumnSignal("passwd", 4),
                                "password match",
                                true
                        )
                )));

        ImportJobAiMappingSuggestionService service = new ImportJobAiMappingSuggestionService(
                importJobQueryUseCase,
                new ImportJobMappingSuggestionPromptBuilder(),
                importJobMappingSuggestionAiProvider,
                new AiInteractionExecutionSupport(recordUseCase),
                com.renda.merchantops.api.support.TestFeatureFlagGateSupport.alwaysEnabledGateService(),
                aiProperties()
        );

        assertThatThrownBy(() -> service.generateMappingSuggestion(1L, 101L, "import-ai-mapping-suggestion-invalid-response-1", 7002L))
                .isInstanceOfSatisfying(BizException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SERVICE_UNAVAILABLE);
                    assertThat(ex.getMessage()).isEqualTo("import ai mapping suggestion is unavailable");
                });

        ArgumentCaptor<AiInteractionRecordCommand> commandCaptor = ArgumentCaptor.forClass(AiInteractionRecordCommand.class);
        verify(recordUseCase).record(commandCaptor.capture());
        assertThat(commandCaptor.getValue().status()).isEqualTo(AiInteractionStatus.INVALID_RESPONSE);
        assertThat(commandCaptor.getValue().interactionType()).isEqualTo("MAPPING_SUGGESTION");
    }

    @Test
    void generateMappingSuggestionShouldRejectObservedColumnSignalOutsideSanitizedHeaderWidth() {
        when(importJobQueryUseCase.getJobDetail(1L, 7002L)).thenReturn(sampleDetailWithHeaderSignal());
        when(importJobQueryUseCase.pageJobErrors(eq(1L), eq(7002L), eq(new ImportJobErrorPageCriteria(0, 20, null))))
                .thenReturn(new ImportJobErrorPageResult(List.of(sampleRowError()), 0, 20, 1, 1));
        when(importJobMappingSuggestionAiProvider.generateMappingSuggestion(any()))
                .thenReturn(providerResult(List.of(
                        mapping("username", "login", 6, "username match", false),
                        mapping("displayName", "display_name", 2, "displayName match", false),
                        mapping("email", "email_address", 3, "email match", false),
                        mapping("password", "passwd", 4, "password match", true),
                        mapping("roleCodes", "roles", 5, "roleCodes match", true)
                )));

        ImportJobAiMappingSuggestionService service = new ImportJobAiMappingSuggestionService(
                importJobQueryUseCase,
                new ImportJobMappingSuggestionPromptBuilder(),
                importJobMappingSuggestionAiProvider,
                new AiInteractionExecutionSupport(recordUseCase),
                com.renda.merchantops.api.support.TestFeatureFlagGateSupport.alwaysEnabledGateService(),
                aiProperties()
        );

        assertThatThrownBy(() -> service.generateMappingSuggestion(1L, 101L, "import-ai-mapping-suggestion-position-range-1", 7002L))
                .isInstanceOfSatisfying(BizException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SERVICE_UNAVAILABLE);
                    assertThat(ex.getMessage()).isEqualTo("import ai mapping suggestion is unavailable");
                });

        ArgumentCaptor<AiInteractionRecordCommand> commandCaptor = ArgumentCaptor.forClass(AiInteractionRecordCommand.class);
        verify(recordUseCase).record(commandCaptor.capture());
        assertThat(commandCaptor.getValue().status()).isEqualTo(AiInteractionStatus.INVALID_RESPONSE);
    }

    @Test
    void generateMappingSuggestionShouldRejectObservedColumnSignalWhenHeaderNameDoesNotMatchLocalSignal() {
        when(importJobQueryUseCase.getJobDetail(1L, 7002L)).thenReturn(sampleDetailWithHeaderSignal());
        when(importJobQueryUseCase.pageJobErrors(eq(1L), eq(7002L), eq(new ImportJobErrorPageCriteria(0, 20, null))))
                .thenReturn(new ImportJobErrorPageResult(List.of(sampleRowError()), 0, 20, 1, 1));
        when(importJobMappingSuggestionAiProvider.generateMappingSuggestion(any()))
                .thenReturn(providerResult(List.of(
                        mapping("username", "display_name", 1, "wrong header at same position", false),
                        mapping("displayName", "display_name", 2, "displayName match", false),
                        mapping("email", "email_address", 3, "email match", false),
                        mapping("password", "passwd", 4, "password match", true),
                        mapping("roleCodes", "roles", 5, "roleCodes match", true)
                )));

        ImportJobAiMappingSuggestionService service = new ImportJobAiMappingSuggestionService(
                importJobQueryUseCase,
                new ImportJobMappingSuggestionPromptBuilder(),
                importJobMappingSuggestionAiProvider,
                new AiInteractionExecutionSupport(recordUseCase),
                com.renda.merchantops.api.support.TestFeatureFlagGateSupport.alwaysEnabledGateService(),
                aiProperties()
        );

        assertThatThrownBy(() -> service.generateMappingSuggestion(1L, 101L, "import-ai-mapping-suggestion-ungrounded-header-1", 7002L))
                .isInstanceOfSatisfying(BizException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SERVICE_UNAVAILABLE);
                    assertThat(ex.getMessage()).isEqualTo("import ai mapping suggestion is unavailable");
                });

        ArgumentCaptor<AiInteractionRecordCommand> commandCaptor = ArgumentCaptor.forClass(AiInteractionRecordCommand.class);
        verify(recordUseCase).record(commandCaptor.capture());
        assertThat(commandCaptor.getValue().status()).isEqualTo(AiInteractionStatus.INVALID_RESPONSE);
    }

    @Test
    void generateMappingSuggestionShouldNormalizeObservedHeaderNameBeforeGroundingIt() {
        when(importJobQueryUseCase.getJobDetail(1L, 7002L)).thenReturn(sampleDetailWithHeaderSignal());
        when(importJobQueryUseCase.pageJobErrors(eq(1L), eq(7002L), eq(new ImportJobErrorPageCriteria(0, 20, null))))
                .thenReturn(new ImportJobErrorPageResult(List.of(sampleRowError()), 0, 20, 1, 1));
        when(importJobMappingSuggestionAiProvider.generateMappingSuggestion(any()))
                .thenReturn(providerResult(List.of(
                        mapping("username", "  LOGIN  ", 1, "username match", false),
                        mapping("displayName", " DISPLAY_NAME ", 2, "displayName match", false),
                        mapping("email", " EMAIL_ADDRESS ", 3, "email match", false),
                        mapping("password", " PASSWD ", 4, "password match", true),
                        mapping("roleCodes", " ROLES ", 5, "roleCodes match", true)
                )));

        ImportJobAiMappingSuggestionService service = new ImportJobAiMappingSuggestionService(
                importJobQueryUseCase,
                new ImportJobMappingSuggestionPromptBuilder(),
                importJobMappingSuggestionAiProvider,
                new AiInteractionExecutionSupport(recordUseCase),
                com.renda.merchantops.api.support.TestFeatureFlagGateSupport.alwaysEnabledGateService(),
                aiProperties()
        );

        var response = service.generateMappingSuggestion(1L, 101L, "import-ai-mapping-suggestion-normalized-header-1", 7002L);

        assertThat(response.suggestedFieldMappings())
                .extracting(item -> item.observedColumnSignal() == null ? null : item.observedColumnSignal().headerName())
                .containsExactly("login", "display_name", "email_address", "passwd", "roles");
    }

    private AiProperties aiProperties() {
        AiProperties aiProperties = new AiProperties();
        aiProperties.setEnabled(true);
        aiProperties.setProvider(AiProviderType.OPENAI);
        aiProperties.setImportMappingSuggestionPromptVersion("import-mapping-suggestion-v1");
        aiProperties.setModelId("gpt-4.1-mini");
        aiProperties.setTimeoutMs(1000);
        aiProperties.setApiKey("test-key");
        aiProperties.setBaseUrl("https://api.openai.com");
        return aiProperties;
    }

    private ImportJobDetail sampleDetailWithHeaderSignal() {
        ImportJobRecord job = new ImportJobRecord(
                7002L,
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
                "header validation failed before row fixes were possible",
                LocalDateTime.of(2026, 3, 27, 9, 0, 0),
                LocalDateTime.of(2026, 3, 27, 9, 0, 2),
                LocalDateTime.of(2026, 3, 27, 9, 0, 5)
        );
        return new ImportJobDetail(
                job,
                List.of(
                        new ImportJobErrorCount("INVALID_HEADER", 1L),
                        new ImportJobErrorCount("UNKNOWN_ROLE", 1L)
                ),
                List.of(sampleHeaderError(), sampleRowError())
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
                LocalDateTime.of(2026, 3, 27, 9, 10, 0),
                LocalDateTime.of(2026, 3, 27, 9, 10, 2),
                LocalDateTime.of(2026, 3, 27, 9, 10, 5)
        );
        return new ImportJobDetail(job, List.of(), List.of());
    }

    private ImportJobDetail sampleDetailWithoutHeaderSignal() {
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
        return new ImportJobDetail(job, List.of(new ImportJobErrorCount("UNKNOWN_ROLE", 2L)), List.of(sampleRowError()));
    }

    private ImportJobErrorRecord sampleHeaderError() {
        return new ImportJobErrorRecord(
                7201L,
                1L,
                7002L,
                null,
                "INVALID_HEADER",
                "header columns do not match USER_CSV",
                "login,display_name,email_address,passwd,roles",
                LocalDateTime.of(2026, 3, 27, 9, 0, 3)
        );
    }

    private ImportJobErrorRecord sampleRowError() {
        return new ImportJobErrorRecord(
                7202L,
                1L,
                7002L,
                2,
                "UNKNOWN_ROLE",
                "roleCodes must exist in current tenant",
                "retry-user,Retry User,retry-user@example.com,secret-pass-1,READ_ONLY",
                LocalDateTime.of(2026, 3, 27, 9, 0, 4)
        );
    }

    private ImportJobMappingSuggestionProviderResult sampleProviderResult() {
        return providerResult(List.of(
                mapping("username", "login", 1, "`login` is the closest observed header for the canonical username field.", false),
                mapping("displayName", "display_name", 2, "`display_name` is the closest semantic match for displayName.", false),
                mapping("email", "email_address", 3, "`email_address` is the most likely email column.", false),
                mapping("password", "passwd", 4, "`passwd` should be manually confirmed.", true),
                mapping("roleCodes", "roles", 5, "`roles` is the closest available roleCodes signal.", true)
        ));
    }

    private ImportJobMappingSuggestionProviderResult providerResult(
            List<ImportJobMappingSuggestionProviderResult.SuggestedFieldMapping> mappings
    ) {
        return new ImportJobMappingSuggestionProviderResult(
                "The failed header still looks close to the canonical USER_CSV schema, so the safest next step is to confirm the proposed field mappings before preparing any replay input.",
                mappings,
                List.of("The source file failed header validation, so each suggested mapping should be reviewed before reuse."),
                List.of(
                        "Confirm the source header order before editing any replay input.",
                        "Verify that the observed `roles` column really contains tenant role codes in the expected delimiter format."
                ),
                "gpt-4.1-mini",
                120,
                40,
                160,
                null
        );
    }

    private ImportJobMappingSuggestionProviderResult.SuggestedFieldMapping mapping(
            String canonicalField,
            String headerName,
            Integer headerPosition,
            String reasoning,
            boolean reviewRequired
    ) {
        return new ImportJobMappingSuggestionProviderResult.SuggestedFieldMapping(
                canonicalField,
                new ImportJobMappingSuggestionProviderResult.ObservedColumnSignal(headerName, headerPosition),
                reasoning,
                reviewRequired
        );
    }
}
