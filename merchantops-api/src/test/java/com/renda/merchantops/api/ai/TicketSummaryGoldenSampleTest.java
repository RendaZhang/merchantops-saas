package com.renda.merchantops.api.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.renda.merchantops.api.config.AiProperties;
import com.renda.merchantops.api.dto.ticket.query.TicketAiSummaryResponse;
import com.renda.merchantops.api.service.TicketAiSummaryService;
import com.renda.merchantops.api.service.TicketQueryService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.client.RestClient;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TicketSummaryGoldenSampleTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void goldenSamplesShouldKeepStableSummaryFormatThroughRealProviderAndServicePath() throws Exception {
        List<GoldenSample> samples = loadSamples();
        assertThat(samples).isNotEmpty();

        for (GoldenSample sample : samples) {
            OpenAiFixtureServer.withServer(200, loadProviderResponse(sample.ticketId()), server -> {
                TicketQueryService ticketQueryService = mock(TicketQueryService.class);
                AiInteractionRecordService recordService = mock(AiInteractionRecordService.class);
                when(ticketQueryService.getTicketPromptContext(sample.tenantId(), sample.ticketId()))
                        .thenReturn(sample.toPromptContext());

                TicketAiSummaryService service = new TicketAiSummaryService(
                        ticketQueryService,
                        new TicketSummaryPromptBuilder(),
                        newProvider(server.baseUrl()),
                        recordService,
                        aiProperties(server.baseUrl())
                );

                TicketAiSummaryResponse response = service.generateSummary(
                        sample.tenantId(),
                        7001L,
                        "golden-summary-" + sample.ticketId(),
                        sample.ticketId()
                );

                assertThat(response.ticketId()).isEqualTo(sample.ticketId());
                assertThat(response.summary()).isEqualTo(sample.expectedSummary());
                assertThat(response.summary()).startsWith("Issue:");
                assertThat(response.summary()).contains("Current:");
                assertThat(response.summary()).contains("Next:");
                assertThat(response.promptVersion()).isEqualTo("ticket-summary-v1");
                assertThat(response.modelId()).isEqualTo("gpt-4.1-mini");
                assertThat(response.generatedAt()).isNotNull();
                assertThat(response.latencyMs()).isNotNegative();
                assertThat(response.requestId()).isEqualTo("golden-summary-" + sample.ticketId());

                JsonNode requestBody = objectMapper.readTree(server.requireCapturedRequest().body());
                assertThat(requestBody.path("input").get(1).path("content").asText()).contains(sample.title());
                assertThat(requestBody.path("input").get(1).path("content").asText()).contains(sample.status());
                assertThat(requestBody.path("input").get(1).path("content").asText()).contains(sample.operationLogs().getFirst().detail());

                ArgumentCaptor<AiInteractionRecordCommand> commandCaptor = ArgumentCaptor.forClass(AiInteractionRecordCommand.class);
                verify(recordService).record(commandCaptor.capture());
                assertThat(commandCaptor.getValue().tenantId()).isEqualTo(sample.tenantId());
                assertThat(commandCaptor.getValue().userId()).isEqualTo(7001L);
                assertThat(commandCaptor.getValue().requestId()).isEqualTo("golden-summary-" + sample.ticketId());
                assertThat(commandCaptor.getValue().entityId()).isEqualTo(sample.ticketId());
                assertThat(commandCaptor.getValue().interactionType()).isEqualTo("SUMMARY");
                assertThat(commandCaptor.getValue().promptVersion()).isEqualTo("ticket-summary-v1");
                assertThat(commandCaptor.getValue().modelId()).isEqualTo("gpt-4.1-mini");
                assertThat(commandCaptor.getValue().status()).isEqualTo(AiInteractionStatus.SUCCEEDED);
                assertThat(commandCaptor.getValue().outputSummary()).isEqualTo(sample.expectedSummary());
            });
        }
    }

    private List<GoldenSample> loadSamples() throws Exception {
        try (InputStream inputStream = getClass().getResourceAsStream("/ai/ticket-summary/golden-samples.json")) {
            assertThat(inputStream).isNotNull();
            return objectMapper.readValue(inputStream, new TypeReference<>() {
            });
        }
    }

    private String loadProviderResponse(Long ticketId) throws Exception {
        try (InputStream inputStream = getClass().getResourceAsStream("/ai/ticket-summary/provider-response-" + ticketId + ".json")) {
            assertThat(inputStream).isNotNull();
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private OpenAiTicketSummaryProvider newProvider(String baseUrl) {
        return new OpenAiTicketSummaryProvider(RestClient.builder(), new ObjectMapper(), aiProperties(baseUrl));
    }

    private AiProperties aiProperties(String baseUrl) {
        AiProperties aiProperties = new AiProperties();
        aiProperties.setEnabled(true);
        aiProperties.setPromptVersion("ticket-summary-v1");
        aiProperties.setModelId("gpt-4.1-mini");
        aiProperties.setTimeoutMs(1000);
        aiProperties.getOpenai().setApiKey("test-key");
        aiProperties.getOpenai().setBaseUrl(baseUrl);
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
            String expectedSummary
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
