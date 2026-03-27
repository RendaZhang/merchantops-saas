package com.renda.merchantops.api.ai.importjob.errorsummary;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.renda.merchantops.api.ai.client.OpenAiTestResponseSupport;
import com.renda.merchantops.api.ai.client.StructuredOutputAiResponse;
import com.renda.merchantops.api.ai.core.AiInteractionExecutionSupport;
import com.renda.merchantops.api.ai.support.StubStructuredOutputAiClient;
import com.renda.merchantops.api.config.AiProperties;
import com.renda.merchantops.api.config.AiProviderType;
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
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImportJobErrorSummaryGoldenSampleTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void goldenSamplesShouldKeepStableImportErrorSummaryFormatThroughRealProviderAndServicePath() throws Exception {
        List<GoldenSample> samples = loadSamples();
        assertThat(samples).isNotEmpty();

        for (GoldenSample sample : samples) {
            ImportJobQueryUseCase importJobQueryUseCase = mock(ImportJobQueryUseCase.class);
            AiInteractionRecordUseCase recordUseCase = mock(AiInteractionRecordUseCase.class);
            StubStructuredOutputAiClient structuredOutputAiClient = new StubStructuredOutputAiClient();
            structuredOutputAiClient.willReturn(loadProviderResponse(sample.importJobId()));
            when(importJobQueryUseCase.getJobDetail(sample.tenantId(), sample.importJobId()))
                    .thenReturn(sample.toDetail());
            when(importJobQueryUseCase.pageJobErrors(sample.tenantId(), sample.importJobId(), new ImportJobErrorPageCriteria(0, 20, null)))
                    .thenReturn(new ImportJobErrorPageResult(sample.toErrorRecords(), 0, 20, sample.errors().size(), 1));

            ImportJobAiErrorSummaryService service = new ImportJobAiErrorSummaryService(
                    importJobQueryUseCase,
                    new ImportJobErrorSummaryPromptBuilder(),
                    new OpenAiImportJobErrorSummaryProvider(new ObjectMapper(), structuredOutputAiClient),
                    new AiInteractionExecutionSupport(recordUseCase),
                    aiProperties()
            );

            var response = service.generateErrorSummary(
                    sample.tenantId(),
                    7001L,
                    "golden-import-error-summary-" + sample.importJobId(),
                    sample.importJobId()
            );

            assertThat(response.importJobId()).isEqualTo(sample.importJobId());
            assertThat(response.summary()).isEqualTo(sample.expectedSummary());
            assertThat(response.topErrorPatterns()).containsExactlyElementsOf(sample.expectedTopErrorPatterns());
            assertThat(response.recommendedNextSteps()).containsExactlyElementsOf(sample.expectedRecommendedNextSteps());
            assertThat(response.promptVersion()).isEqualTo("import-error-summary-v1");
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
            assertThat(commandCaptor.getValue().entityType()).isEqualTo("IMPORT_JOB");
            assertThat(commandCaptor.getValue().entityId()).isEqualTo(sample.importJobId());
            assertThat(commandCaptor.getValue().interactionType()).isEqualTo("ERROR_SUMMARY");
            assertThat(commandCaptor.getValue().status()).isEqualTo(AiInteractionStatus.SUCCEEDED);
            assertThat(commandCaptor.getValue().outputSummary()).isEqualTo(sample.expectedSummary());
        }
    }

    private List<GoldenSample> loadSamples() throws Exception {
        try (InputStream inputStream = getClass().getResourceAsStream("/ai/import-job-error-summary/golden-samples.json")) {
            assertThat(inputStream).isNotNull();
            return objectMapper.readValue(inputStream, new TypeReference<>() {
            });
        }
    }

    private StructuredOutputAiResponse loadProviderResponse(Long importJobId) throws Exception {
        try (InputStream inputStream = getClass().getResourceAsStream("/ai/import-job-error-summary/provider-response-" + importJobId + ".json")) {
            assertThat(inputStream).isNotNull();
            JsonNode root = objectMapper.readTree(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
            String rawText = OpenAiTestResponseSupport.extractOutputText(root);
            JsonNode usage = root.path("usage");
            return new StructuredOutputAiResponse(
                    rawText,
                    root.path("model").asText("gpt-4.1-mini"),
                    usage.hasNonNull("input_tokens") ? usage.get("input_tokens").asInt() : null,
                    usage.hasNonNull("output_tokens") ? usage.get("output_tokens").asInt() : null,
                    usage.hasNonNull("total_tokens") ? usage.get("total_tokens").asInt() : null,
                    null
            );
        }
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
}
