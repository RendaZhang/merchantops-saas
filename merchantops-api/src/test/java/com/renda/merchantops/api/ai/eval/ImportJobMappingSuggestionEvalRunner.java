package com.renda.merchantops.api.ai.eval;

import com.fasterxml.jackson.core.type.TypeReference;
import com.renda.merchantops.api.ai.core.AiInteractionExecutionSupport;
import com.renda.merchantops.api.ai.importjob.mappingsuggestion.ImportJobMappingSuggestionAiProvider;
import com.renda.merchantops.api.ai.importjob.mappingsuggestion.ImportJobMappingSuggestionProviderRequest;
import com.renda.merchantops.api.ai.importjob.mappingsuggestion.ImportJobMappingSuggestionProviderResult;
import com.renda.merchantops.api.ai.importjob.mappingsuggestion.OpenAiImportJobMappingSuggestionProvider;
import com.renda.merchantops.api.ai.importjob.mappingsuggestion.ImportJobMappingSuggestionPromptBuilder;
import com.renda.merchantops.api.ai.support.StubStructuredOutputAiClient;
import com.renda.merchantops.api.dto.importjob.query.ImportJobAiMappingSuggestionResponse;
import com.renda.merchantops.api.importjob.ai.ImportJobAiMappingSuggestionService;
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
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

final class ImportJobMappingSuggestionEvalRunner extends AbstractAiWorkflowEvalRunner {

    private static final long USER_ID = 7001L;

    @Override
    public AiWorkflowEvalResult run(AiWorkflowEvalInventoryEntry inventoryEntry,
                                    EnumSet<AiWorkflowEvalSampleType> sampleTypes) throws Exception {
        List<AiWorkflowEvalFailure> failures = new ArrayList<>();
        int goldenCount = 0;
        int failureCount = 0;
        int policyCount = 0;

        if (sampleTypes.contains(AiWorkflowEvalSampleType.GOLDEN)) {
            List<GoldenSample> samples = loadList(inventoryEntry.goldenSamplesPath(), new TypeReference<>() {
            });
            goldenCount = samples.size();
            for (GoldenSample sample : samples) {
                runSample(failures, inventoryEntry, AiWorkflowEvalSampleType.GOLDEN, String.valueOf(sample.importJobId()),
                        () -> assertGoldenSample(inventoryEntry, sample));
            }
        }

        if (sampleTypes.contains(AiWorkflowEvalSampleType.FAILURE)) {
            List<FailureSample> samples = loadList(inventoryEntry.failureSamplesPath(), new TypeReference<>() {
            });
            failureCount = samples.size();
            for (FailureSample sample : samples) {
                runSample(failures, inventoryEntry, AiWorkflowEvalSampleType.FAILURE, sample.id(),
                        () -> assertFailureSample(inventoryEntry, sample));
            }
        }

        if (sampleTypes.contains(AiWorkflowEvalSampleType.POLICY)) {
            List<PolicySample> samples = loadList(inventoryEntry.policySamplesPath(), new TypeReference<>() {
            });
            policyCount = samples.size();
            for (PolicySample sample : samples) {
                runSample(failures, inventoryEntry, AiWorkflowEvalSampleType.POLICY, sample.id(),
                        () -> assertPolicySample(inventoryEntry, sample));
            }
        }

        return result(inventoryEntry, goldenCount, failureCount, policyCount, failures);
    }

    private void assertGoldenSample(AiWorkflowEvalInventoryEntry inventoryEntry, GoldenSample sample) throws Exception {
        ImportJobQueryUseCase importJobQueryUseCase = mock(ImportJobQueryUseCase.class);
        AiInteractionRecordUseCase recordUseCase = mock(AiInteractionRecordUseCase.class);
        StubStructuredOutputAiClient structuredOutputAiClient = new StubStructuredOutputAiClient();
        structuredOutputAiClient.willReturn(loadStructuredOutputResponse(
                "/ai/import-job-mapping-suggestion/provider-response-" + sample.importJobId() + ".json"
        ));
        when(importJobQueryUseCase.getJobDetail(sample.tenantId(), sample.importJobId()))
                .thenReturn(sample.toDetail());
        when(importJobQueryUseCase.pageJobErrors(sample.tenantId(), sample.importJobId(), new ImportJobErrorPageCriteria(0, 20, null)))
                .thenReturn(new ImportJobErrorPageResult(sample.toPromptWindowErrors(), 0, 20, sample.errors().size(), 1));

        ImportJobAiMappingSuggestionService service = new ImportJobAiMappingSuggestionService(
                importJobQueryUseCase,
                new ImportJobMappingSuggestionPromptBuilder(),
                new OpenAiImportJobMappingSuggestionProvider(objectMapper, structuredOutputAiClient),
                new AiInteractionExecutionSupport(recordUseCase),
                aiProperties(inventoryEntry.workflow())
        );

        var response = service.generateMappingSuggestion(
                sample.tenantId(),
                USER_ID,
                "golden-import-mapping-suggestion-" + sample.importJobId(),
                sample.importJobId()
        );

        assertThat(response.importJobId()).isEqualTo(sample.importJobId());
        assertThat(response.summary()).isEqualTo(sample.expectedSummary());
        assertThat(response.suggestedFieldMappings()).containsExactlyElementsOf(sample.expectedSuggestedFieldMappings());
        assertThat(response.confidenceNotes()).containsExactlyElementsOf(sample.expectedConfidenceNotes());
        assertThat(response.recommendedOperatorChecks()).containsExactlyElementsOf(sample.expectedRecommendedOperatorChecks());
        assertThat(response.promptVersion()).isEqualTo(inventoryEntry.expectedPromptVersion());
        assertThat(response.modelId()).isEqualTo("gpt-4.1-mini");
        assertThat(response.generatedAt()).isNotNull();
        assertThat(response.latencyMs()).isNotNegative();
        assertThat(response.requestId()).isEqualTo("golden-import-mapping-suggestion-" + sample.importJobId());

        String prompt = structuredOutputAiClient.lastRequest().userPrompt();
        assertThat(prompt).contains(sample.job().sourceFilename());
        assertThat(prompt).contains("sourceErrorCode: INVALID_HEADER");
        assertThat(prompt).contains("headerPosition: 1; headerName: login");
        assertThat(prompt).contains("passwordPresent=true");
        assertThat(prompt).doesNotContain("login,display_name,email_address,passwd,roles");
        assertThat(prompt).doesNotContain(sample.errors().stream().filter(item -> item.rowNumber() != null).findFirst().orElseThrow().rawPayload());
        assertThat(prompt).doesNotContain("abc123");
        assertThat(prompt).doesNotContain("retry-role@example.com");

        ArgumentCaptor<AiInteractionRecordCommand> commandCaptor = ArgumentCaptor.forClass(AiInteractionRecordCommand.class);
        verify(recordUseCase).record(commandCaptor.capture());
        assertThat(commandCaptor.getValue().requestId()).isEqualTo("golden-import-mapping-suggestion-" + sample.importJobId());
        assertThat(commandCaptor.getValue().entityType()).isEqualTo(inventoryEntry.workflow().entityType());
        assertThat(commandCaptor.getValue().entityId()).isEqualTo(sample.importJobId());
        assertThat(commandCaptor.getValue().interactionType()).isEqualTo(inventoryEntry.workflow().interactionType());
        assertThat(commandCaptor.getValue().status()).isEqualTo(AiInteractionStatus.SUCCEEDED);
        assertThat(commandCaptor.getValue().outputSummary()).isEqualTo(sample.expectedSummary());
    }

    private void assertFailureSample(AiWorkflowEvalInventoryEntry inventoryEntry, FailureSample sample) {
        ImportJobQueryUseCase importJobQueryUseCase = mock(ImportJobQueryUseCase.class);
        ImportJobMappingSuggestionAiProvider importJobMappingSuggestionAiProvider = mock(ImportJobMappingSuggestionAiProvider.class);
        AiInteractionRecordUseCase recordUseCase = mock(AiInteractionRecordUseCase.class);
        when(importJobQueryUseCase.getJobDetail(sample.detail().job().tenantId(), sample.detail().job().id())).thenReturn(sample.detail());
        when(importJobQueryUseCase.pageJobErrors(
                eq(sample.detail().job().tenantId()),
                eq(sample.detail().job().id()),
                eq(new ImportJobErrorPageCriteria(0, 20, null))
        )).thenReturn(new ImportJobErrorPageResult(sample.promptWindowErrors(), 0, 20, sample.promptWindowErrors().size(), 1));
        if (sample.providerResult() != null) {
            when(importJobMappingSuggestionAiProvider.generateMappingSuggestion(any())).thenReturn(sample.providerResult());
        }

        ImportJobAiMappingSuggestionService service = new ImportJobAiMappingSuggestionService(
                importJobQueryUseCase,
                new ImportJobMappingSuggestionPromptBuilder(),
                importJobMappingSuggestionAiProvider,
                new AiInteractionExecutionSupport(recordUseCase),
                aiProperties(inventoryEntry.workflow())
        );

        assertThatThrownBy(() -> service.generateMappingSuggestion(
                sample.detail().job().tenantId(),
                USER_ID,
                "failure-import-mapping-suggestion-" + sample.id(),
                sample.detail().job().id()
        )).isInstanceOfSatisfying(BizException.class, ex -> {
            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.valueOf(sample.expectedErrorCode()));
            assertThat(ex.getMessage()).isEqualTo(sample.expectedMessage());
        });

        if (!sample.expectInteractionRecord()) {
            verifyNoInteractions(recordUseCase);
            verifyNoInteractions(importJobMappingSuggestionAiProvider);
            return;
        }

        ArgumentCaptor<AiInteractionRecordCommand> commandCaptor = ArgumentCaptor.forClass(AiInteractionRecordCommand.class);
        verify(recordUseCase).record(commandCaptor.capture());
        assertThat(commandCaptor.getValue().status()).isEqualTo(AiInteractionStatus.valueOf(sample.expectedStatus()));
    }

    private void assertPolicySample(AiWorkflowEvalInventoryEntry inventoryEntry, PolicySample sample) {
        ImportJobQueryUseCase importJobQueryUseCase = mock(ImportJobQueryUseCase.class);
        ImportJobMappingSuggestionAiProvider importJobMappingSuggestionAiProvider = mock(ImportJobMappingSuggestionAiProvider.class);
        AiInteractionRecordUseCase recordUseCase = mock(AiInteractionRecordUseCase.class);
        when(importJobQueryUseCase.getJobDetail(sample.detail().job().tenantId(), sample.detail().job().id())).thenReturn(sample.detail());
        when(importJobQueryUseCase.pageJobErrors(
                eq(sample.detail().job().tenantId()),
                eq(sample.detail().job().id()),
                eq(new ImportJobErrorPageCriteria(0, 20, null))
        )).thenReturn(new ImportJobErrorPageResult(sample.promptWindowErrors(), 0, 20, sample.promptWindowErrors().size(), 1));
        when(importJobMappingSuggestionAiProvider.generateMappingSuggestion(any())).thenReturn(sample.providerResult());

        ImportJobAiMappingSuggestionService service = new ImportJobAiMappingSuggestionService(
                importJobQueryUseCase,
                new ImportJobMappingSuggestionPromptBuilder(),
                importJobMappingSuggestionAiProvider,
                new AiInteractionExecutionSupport(recordUseCase),
                aiProperties(inventoryEntry.workflow())
        );

        var response = service.generateMappingSuggestion(
                sample.detail().job().tenantId(),
                USER_ID,
                "policy-import-mapping-suggestion-" + sample.id(),
                sample.detail().job().id()
        );

        ArgumentCaptor<ImportJobMappingSuggestionProviderRequest> requestCaptor =
                ArgumentCaptor.forClass(ImportJobMappingSuggestionProviderRequest.class);
        verify(importJobMappingSuggestionAiProvider).generateMappingSuggestion(requestCaptor.capture());
        String userPrompt = requestCaptor.getValue().prompt().userPrompt();
        assertThat(userPrompt).contains(sample.promptMustContain().toArray(String[]::new));
        assertThat(userPrompt).doesNotContain(sample.promptMustNotContain().toArray(String[]::new));
        assertThat(response.summary()).doesNotContain(sample.outputMustNotContain().toArray(String[]::new));
        assertThat(response.suggestedFieldMappings())
                .filteredOn(ImportJobAiMappingSuggestionResponse.SuggestedFieldMapping::reviewRequired)
                .extracting(ImportJobAiMappingSuggestionResponse.SuggestedFieldMapping::canonicalField)
                .containsExactlyInAnyOrderElementsOf(sample.reviewRequiredCanonicalFields());

        ArgumentCaptor<AiInteractionRecordCommand> commandCaptor = ArgumentCaptor.forClass(AiInteractionRecordCommand.class);
        verify(recordUseCase).record(commandCaptor.capture());
        assertThat(commandCaptor.getValue().status()).isEqualTo(AiInteractionStatus.SUCCEEDED);
    }

    private record GoldenSample(
            Long importJobId,
            Long tenantId,
            JobSample job,
            List<ErrorCodeCountSample> errorCodeCounts,
            List<ErrorSample> errors,
            String expectedSummary,
            List<ImportJobAiMappingSuggestionResponse.SuggestedFieldMapping> expectedSuggestedFieldMappings,
            List<String> expectedConfidenceNotes,
            List<String> expectedRecommendedOperatorChecks
    ) {
        private ImportJobDetail toDetail() {
            return new ImportJobDetail(
                    job.toRecord(importJobId, tenantId),
                    errorCodeCounts.stream().map(ErrorCodeCountSample::toRecord).toList(),
                    errors.stream().map(error -> error.toRecord(importJobId, tenantId)).toList()
            );
        }

        private List<ImportJobErrorRecord> toPromptWindowErrors() {
            return errors.stream()
                    .filter(error -> error.rowNumber() != null)
                    .map(error -> error.toRecord(importJobId, tenantId))
                    .toList();
        }
    }

    private record JobSample(
            String importType,
            String sourceType,
            String sourceFilename,
            String storageKey,
            Long sourceJobId,
            String status,
            Long requestedBy,
            String requestId,
            Integer totalCount,
            Integer successCount,
            Integer failureCount,
            String errorSummary,
            LocalDateTime createdAt,
            LocalDateTime startedAt,
            LocalDateTime finishedAt
    ) {
        private ImportJobRecord toRecord(Long importJobId, Long tenantId) {
            return new ImportJobRecord(
                    importJobId,
                    tenantId,
                    importType,
                    sourceType,
                    sourceFilename,
                    storageKey,
                    sourceJobId,
                    status,
                    requestedBy,
                    requestId,
                    totalCount,
                    successCount,
                    failureCount,
                    errorSummary,
                    createdAt,
                    startedAt,
                    finishedAt
            );
        }
    }

    private record ErrorCodeCountSample(
            String errorCode,
            long count
    ) {
        private ImportJobErrorCount toRecord() {
            return new ImportJobErrorCount(errorCode, count);
        }
    }

    private record ErrorSample(
            Long id,
            Integer rowNumber,
            String errorCode,
            String errorMessage,
            String rawPayload,
            LocalDateTime createdAt
    ) {
        private ImportJobErrorRecord toRecord(Long importJobId, Long tenantId) {
            return new ImportJobErrorRecord(id, tenantId, importJobId, rowNumber, errorCode, errorMessage, rawPayload, createdAt);
        }
    }

    private record FailureSample(
            String id,
            ImportJobDetail detail,
            List<ImportJobErrorRecord> promptWindowErrors,
            ImportJobMappingSuggestionProviderResult providerResult,
            boolean expectInteractionRecord,
            String expectedStatus,
            String expectedErrorCode,
            String expectedMessage
    ) {
    }

    private record PolicySample(
            String id,
            ImportJobDetail detail,
            List<ImportJobErrorRecord> promptWindowErrors,
            ImportJobMappingSuggestionProviderResult providerResult,
            List<String> promptMustContain,
            List<String> promptMustNotContain,
            List<String> outputMustNotContain,
            List<String> reviewRequiredCanonicalFields
    ) {
    }
}
