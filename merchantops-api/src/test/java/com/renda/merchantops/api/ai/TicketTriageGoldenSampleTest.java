package com.renda.merchantops.api.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.renda.merchantops.api.config.AiProperties;
import com.renda.merchantops.api.config.AiProviderType;
import com.renda.merchantops.api.dto.ticket.query.TicketAiTriagePriority;
import com.renda.merchantops.api.dto.ticket.query.TicketAiTriageResponse;
import com.renda.merchantops.api.service.TicketAiTriageService;
import com.renda.merchantops.api.service.TicketQueryService;
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

class TicketTriageGoldenSampleTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void goldenSamplesShouldKeepStableTriageFormatThroughRealProviderAndServicePath() throws Exception {
        List<GoldenSample> samples = loadSamples();
        assertThat(samples).isNotEmpty();

        for (GoldenSample sample : samples) {
            TicketQueryService ticketQueryService = mock(TicketQueryService.class);
            AiInteractionRecordService recordService = mock(AiInteractionRecordService.class);
            StubStructuredOutputAiClient structuredOutputAiClient = new StubStructuredOutputAiClient();
            structuredOutputAiClient.willReturn(loadProviderResponse(sample.ticketId()));
            when(ticketQueryService.getTicketPromptContext(sample.tenantId(), sample.ticketId()))
                    .thenReturn(sample.toPromptContext());

            TicketAiTriageService service = new TicketAiTriageService(
                    ticketQueryService,
                    new TicketTriagePromptBuilder(),
                    new OpenAiTicketTriageProvider(new ObjectMapper(), structuredOutputAiClient),
                    recordService,
                    aiProperties()
            );

            TicketAiTriageResponse response = service.generateTriage(
                    sample.tenantId(),
                    7002L,
                    "golden-triage-" + sample.ticketId(),
                    sample.ticketId()
            );

            assertThat(response.ticketId()).isEqualTo(sample.ticketId());
            assertThat(response.classification()).isEqualTo(sample.expectedClassification());
            assertThat(response.priority()).isEqualTo(sample.expectedPriority());
            assertThat(response.reasoning()).isEqualTo(sample.expectedReasoning());
            assertThat(response.priority()).isIn(TicketAiTriagePriority.LOW, TicketAiTriagePriority.MEDIUM, TicketAiTriagePriority.HIGH);
            assertThat(response.reasoning()).isNotBlank();
            assertThat(response.promptVersion()).isEqualTo("ticket-triage-v1");
            assertThat(response.modelId()).isEqualTo("gpt-4.1-mini");
            assertThat(response.generatedAt()).isNotNull();
            assertThat(response.latencyMs()).isNotNegative();
            assertThat(response.requestId()).isEqualTo("golden-triage-" + sample.ticketId());

            assertThat(structuredOutputAiClient.lastRequest().userPrompt()).contains(sample.title());
            assertThat(structuredOutputAiClient.lastRequest().userPrompt()).contains(sample.status());
            assertThat(structuredOutputAiClient.lastRequest().userPrompt()).contains(sample.operationLogs().getFirst().detail());

            ArgumentCaptor<AiInteractionRecordCommand> commandCaptor = ArgumentCaptor.forClass(AiInteractionRecordCommand.class);
            verify(recordService).record(commandCaptor.capture());
            assertThat(commandCaptor.getValue().tenantId()).isEqualTo(sample.tenantId());
            assertThat(commandCaptor.getValue().userId()).isEqualTo(7002L);
            assertThat(commandCaptor.getValue().requestId()).isEqualTo("golden-triage-" + sample.ticketId());
            assertThat(commandCaptor.getValue().entityId()).isEqualTo(sample.ticketId());
            assertThat(commandCaptor.getValue().interactionType()).isEqualTo("TRIAGE");
            assertThat(commandCaptor.getValue().promptVersion()).isEqualTo("ticket-triage-v1");
            assertThat(commandCaptor.getValue().modelId()).isEqualTo("gpt-4.1-mini");
            assertThat(commandCaptor.getValue().status()).isEqualTo(AiInteractionStatus.SUCCEEDED);
            assertThat(commandCaptor.getValue().outputSummary())
                    .isEqualTo("classification=" + sample.expectedClassification() + "; priority=" + sample.expectedPriority().name());
        }
    }

    private List<GoldenSample> loadSamples() throws Exception {
        try (InputStream inputStream = getClass().getResourceAsStream("/ai/ticket-triage/golden-samples.json")) {
            assertThat(inputStream).isNotNull();
            return objectMapper.readValue(inputStream, new TypeReference<>() {
            });
        }
    }

    private StructuredOutputAiResponse loadProviderResponse(Long ticketId) throws Exception {
        try (InputStream inputStream = getClass().getResourceAsStream("/ai/ticket-triage/provider-response-" + ticketId + ".json")) {
            assertThat(inputStream).isNotNull();
            JsonNode root = objectMapper.readTree(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
            String rawText = AiProviderHttpSupport.extractOpenAiOutputText(root.path("output")).text();
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
        aiProperties.setTriagePromptVersion("ticket-triage-v1");
        aiProperties.setModelId("gpt-4.1-mini");
        aiProperties.setTimeoutMs(1000);
        aiProperties.setProvider(AiProviderType.OPENAI);
        aiProperties.setApiKey("test-key");
        aiProperties.setBaseUrl("https://api.openai.com");
        return aiProperties;
    }

    private record GoldenSample(
            Long ticketId,
            Long tenantId,
            String title,
            String description,
            String status,
            String assigneeUsername,
            Long createdBy,
            String createdByUsername,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            List<GoldenComment> comments,
            List<GoldenOperationLog> operationLogs,
            String expectedClassification,
            TicketAiTriagePriority expectedPriority,
            String expectedReasoning
    ) {
        private TicketAiPromptContext toPromptContext() {
            return new TicketAiPromptContext(
                    ticketId,
                    tenantId,
                    title,
                    description,
                    status,
                    assigneeUsername,
                    createdByUsername,
                    createdAt,
                    updatedAt,
                    comments.stream()
                            .map(comment -> new TicketAiPromptContext.Comment(
                                    comment.id(),
                                    comment.content(),
                                    comment.createdByUsername(),
                                    comment.createdAt()
                            ))
                            .toList(),
                    false,
                    operationLogs.stream()
                            .map(log -> new TicketAiPromptContext.OperationLog(
                                    log.id(),
                                    log.operationType(),
                                    log.detail(),
                                    log.operatorUsername(),
                                    log.createdAt()
                            ))
                            .toList(),
                    false
            );
        }
    }

    private record GoldenComment(
            Long id,
            Long ticketId,
            String content,
            Long createdBy,
            String createdByUsername,
            LocalDateTime createdAt
    ) {
    }

    private record GoldenOperationLog(
            Long id,
            String operationType,
            String detail,
            Long operatorId,
            String operatorUsername,
            LocalDateTime createdAt
    ) {
    }
}
