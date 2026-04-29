package com.renda.merchantops.api.ai.eval;

import com.fasterxml.jackson.core.type.TypeReference;
import com.renda.merchantops.api.ai.core.AiInteractionExecutionSupport;
import com.renda.merchantops.api.ai.importjob.errorsummary.ImportJobErrorSummaryAiProvider;
import com.renda.merchantops.api.ai.importjob.errorsummary.ImportJobErrorSummaryPromptBuilder;
import com.renda.merchantops.api.ai.importjob.errorsummary.ImportJobErrorSummaryProviderResult;
import com.renda.merchantops.api.ai.importjob.errorsummary.OpenAiImportJobErrorSummaryProvider;
import com.renda.merchantops.api.ai.support.StubStructuredOutputAiClient;
import com.renda.merchantops.api.importjob.ai.ImportJobAiErrorSummaryService;
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
import static org.mockito.Mockito.when;

final class ImportJobErrorSummaryEvalRunner extends AbstractAiWorkflowEvalRunner {

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
                "/ai/import-job-error-summary/provider-response-" + sample.importJobId() + ".json"
        ));
        when(importJobQueryUseCase.getJobDetail(sample.tenantId(), sample.importJobId()))
                .thenReturn(sample.toDetail());
        when(importJobQueryUseCase.pageJobErrors(sample.tenantId(), sample.importJobId(), new ImportJobErrorPageCriteria(0, 20, null)))
                .thenReturn(new ImportJobErrorPageResult(sample.toErrorRecords(), 0, 20, sample.errors().size(), 1));

        ImportJobAiErrorSummaryService service = new ImportJobAiErrorSummaryService(
                importJobQueryUseCase,
                new ImportJobErrorSummaryPromptBuilder(),
                new OpenAiImportJobErrorSummaryProvider(objectMapper, structuredOutputAiClient),
                new AiInteractionExecutionSupport(recordUseCase),
                com.renda.merchantops.api.support.TestFeatureFlagGateSupport.alwaysEnabledGateService(),
                aiProperties(inventoryEntry.workflow())
        );

        var response = service.generateErrorSummary(
                sample.tenantId(),
                USER_ID,
                "golden-import-error-summary-" + sample.importJobId(),
                sample.importJobId()
        );

        assertThat(response.importJobId()).isEqualTo(sample.importJobId());
        assertThat(response.summary()).isEqualTo(sample.expectedSummary());
        assertThat(response.topErrorPatterns()).containsExactlyElementsOf(sample.expectedTopErrorPatterns());
        assertThat(response.recommendedNextSteps()).containsExactlyElementsOf(sample.expectedRecommendedNextSteps());
        assertThat(response.promptVersion()).isEqualTo(inventoryEntry.expectedPromptVersion());
        assertThat(response.modelId()).isEqualTo("gpt-4.1-mini");
        assertThat(response.generatedAt()).isNotNull();
        assertThat(response.latencyMs()).isNotNegative();
        assertThat(response.requestId()).isEqualTo("golden-import-error-summary-" + sample.importJobId());

        String prompt = structuredOutputAiClient.lastRequest().userPrompt();
        assertThat(prompt).contains(sample.job().sourceFilename());
        assertThat(prompt).contains(sample.errorCodeCounts().getFirst().errorCode());
        assertThat(prompt).contains("passwordPresent=true");
        assertThat(prompt).doesNotContain(sample.errors().getFirst().rawPayload());
        assertThat(prompt).doesNotContain("abc123");
        assertThat(prompt).doesNotContain("retry-role@example.com");

        ArgumentCaptor<AiInteractionRecordCommand> commandCaptor = ArgumentCaptor.forClass(AiInteractionRecordCommand.class);
        verify(recordUseCase).record(commandCaptor.capture());
        assertThat(commandCaptor.getValue().requestId()).isEqualTo("golden-import-error-summary-" + sample.importJobId());
        assertThat(commandCaptor.getValue().entityType()).isEqualTo(inventoryEntry.workflow().entityType());
        assertThat(commandCaptor.getValue().entityId()).isEqualTo(sample.importJobId());
        assertThat(commandCaptor.getValue().interactionType()).isEqualTo(inventoryEntry.workflow().interactionType());
        assertThat(commandCaptor.getValue().status()).isEqualTo(AiInteractionStatus.SUCCEEDED);
        assertThat(commandCaptor.getValue().outputSummary()).isEqualTo(sample.expectedSummary());
    }

    private void assertFailureSample(AiWorkflowEvalInventoryEntry inventoryEntry, FailureSample sample) {
        ImportJobQueryUseCase importJobQueryUseCase = mock(ImportJobQueryUseCase.class);
        ImportJobErrorSummaryAiProvider importJobErrorSummaryAiProvider = mock(ImportJobErrorSummaryAiProvider.class);
        AiInteractionRecordUseCase recordUseCase = mock(AiInteractionRecordUseCase.class);
        when(importJobQueryUseCase.getJobDetail(sample.detail().job().tenantId(), sample.detail().job().id())).thenReturn(sample.detail());
        when(importJobQueryUseCase.pageJobErrors(
                eq(sample.detail().job().tenantId()),
                eq(sample.detail().job().id()),
                eq(new ImportJobErrorPageCriteria(0, 20, null))
        )).thenReturn(new ImportJobErrorPageResult(sample.promptWindowErrors(), 0, 20, sample.promptWindowErrors().size(), 1));
        when(importJobErrorSummaryAiProvider.generateErrorSummary(any())).thenReturn(sample.providerResult());

        ImportJobAiErrorSummaryService service = new ImportJobAiErrorSummaryService(
                importJobQueryUseCase,
                new ImportJobErrorSummaryPromptBuilder(),
                importJobErrorSummaryAiProvider,
                new AiInteractionExecutionSupport(recordUseCase),
                com.renda.merchantops.api.support.TestFeatureFlagGateSupport.alwaysEnabledGateService(),
                aiProperties(inventoryEntry.workflow())
        );

        assertThatThrownBy(() -> service.generateErrorSummary(
                sample.detail().job().tenantId(),
                USER_ID,
                "failure-import-error-summary-" + sample.id(),
                sample.detail().job().id()
        )).isInstanceOfSatisfying(BizException.class, ex -> {
            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SERVICE_UNAVAILABLE);
            assertThat(ex.getMessage()).isEqualTo(sample.expectedMessage());
        });

        ArgumentCaptor<AiInteractionRecordCommand> commandCaptor = ArgumentCaptor.forClass(AiInteractionRecordCommand.class);
        verify(recordUseCase).record(commandCaptor.capture());
        assertThat(commandCaptor.getValue().status()).isEqualTo(AiInteractionStatus.valueOf(sample.expectedStatus()));
    }

    private void assertPolicySample(AiWorkflowEvalInventoryEntry inventoryEntry, PolicySample sample) {
        ImportJobQueryUseCase importJobQueryUseCase = mock(ImportJobQueryUseCase.class);
        ImportJobErrorSummaryAiProvider importJobErrorSummaryAiProvider = mock(ImportJobErrorSummaryAiProvider.class);
        AiInteractionRecordUseCase recordUseCase = mock(AiInteractionRecordUseCase.class);
        when(importJobQueryUseCase.getJobDetail(sample.detail().job().tenantId(), sample.detail().job().id())).thenReturn(sample.detail());
        when(importJobQueryUseCase.pageJobErrors(
                eq(sample.detail().job().tenantId()),
                eq(sample.detail().job().id()),
                eq(new ImportJobErrorPageCriteria(0, 20, null))
        )).thenReturn(new ImportJobErrorPageResult(sample.promptWindowErrors(), 0, 20, sample.promptWindowErrors().size(), 1));
        when(importJobErrorSummaryAiProvider.generateErrorSummary(any())).thenReturn(sample.providerResult());

        ImportJobAiErrorSummaryService service = new ImportJobAiErrorSummaryService(
                importJobQueryUseCase,
                new ImportJobErrorSummaryPromptBuilder(),
                importJobErrorSummaryAiProvider,
                new AiInteractionExecutionSupport(recordUseCase),
                com.renda.merchantops.api.support.TestFeatureFlagGateSupport.alwaysEnabledGateService(),
                aiProperties(inventoryEntry.workflow())
        );

        var response = service.generateErrorSummary(
                sample.detail().job().tenantId(),
                USER_ID,
                "policy-import-error-summary-" + sample.id(),
                sample.detail().job().id()
        );

        ArgumentCaptor<com.renda.merchantops.api.ai.importjob.errorsummary.ImportJobErrorSummaryProviderRequest> requestCaptor =
                ArgumentCaptor.forClass(com.renda.merchantops.api.ai.importjob.errorsummary.ImportJobErrorSummaryProviderRequest.class);
        verify(importJobErrorSummaryAiProvider).generateErrorSummary(requestCaptor.capture());
        String userPrompt = requestCaptor.getValue().prompt().userPrompt();
        assertThat(userPrompt).contains(sample.promptMustContain().toArray(String[]::new));
        assertThat(userPrompt).doesNotContain(sample.promptMustNotContain().toArray(String[]::new));
        assertThat(response.summary()).doesNotContain(sample.outputMustNotContain().toArray(String[]::new));

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
            List<String> expectedTopErrorPatterns,
            List<String> expectedRecommendedNextSteps
    ) {
        private ImportJobDetail toDetail() {
            return new ImportJobDetail(job.toRecord(importJobId, tenantId), errorCodeCounts.stream().map(ErrorCodeCountSample::toRecord).toList(), List.of());
        }

        private List<ImportJobErrorRecord> toErrorRecords() {
            return errors.stream().map(error -> error.toRecord(importJobId, tenantId)).toList();
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
            ImportJobErrorSummaryProviderResult providerResult,
            String expectedStatus,
            String expectedMessage
    ) {
    }

    private record PolicySample(
            String id,
            ImportJobDetail detail,
            List<ImportJobErrorRecord> promptWindowErrors,
            ImportJobErrorSummaryProviderResult providerResult,
            List<String> promptMustContain,
            List<String> promptMustNotContain,
            List<String> outputMustNotContain
    ) {
    }
}
