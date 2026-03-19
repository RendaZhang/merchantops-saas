package com.renda.merchantops.api.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.renda.merchantops.api.dto.ticket.query.TicketCommentResponse;
import com.renda.merchantops.api.dto.ticket.query.TicketDetailResponse;
import com.renda.merchantops.api.dto.ticket.query.TicketOperationLogResponse;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TicketSummaryGoldenSampleTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void goldenSamplesShouldKeepStableStubSummaryFormat() throws Exception {
        List<GoldenSample> samples = loadSamples();
        TicketSummaryPromptBuilder promptBuilder = new TicketSummaryPromptBuilder();
        TicketSummaryAiProvider stubProvider = request -> samples.stream()
                .filter(sample -> sample.ticketId().equals(request.ticketId()))
                .findFirst()
                .map(sample -> new TicketSummaryProviderResult(sample.expectedSummary(), "stub-summary-v1", null, null, null, null))
                .orElseThrow();

        for (GoldenSample sample : samples) {
            TicketDetailResponse ticket = sample.toTicketDetailResponse();
            TicketSummaryPrompt prompt = promptBuilder.build("ticket-summary-v1", ticket);
            TicketSummaryProviderResult result = stubProvider.generateSummary(
                    new TicketSummaryProviderRequest("golden-" + sample.ticketId(), sample.ticketId(), "stub-summary-v1", 1000, prompt)
            );

            assertThat(prompt.userPrompt()).contains(sample.title());
            assertThat(prompt.userPrompt()).contains(sample.status());
            assertThat(prompt.userPrompt()).contains(sample.operationLogs().getFirst().detail());
            assertThat(result.summary()).isEqualTo(sample.expectedSummary());
            assertThat(result.summary()).startsWith("Issue:");
            assertThat(result.summary()).contains("Current:");
            assertThat(result.summary()).contains("Next:");
        }
    }

    private List<GoldenSample> loadSamples() throws Exception {
        try (InputStream inputStream = getClass().getResourceAsStream("/ai/ticket-summary/golden-samples.json")) {
            assertThat(inputStream).isNotNull();
            return objectMapper.readValue(inputStream, new TypeReference<>() {
            });
        }
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
        private TicketDetailResponse toTicketDetailResponse() {
            return new TicketDetailResponse(
                    ticketId,
                    tenantId,
                    title,
                    description,
                    status,
                    null,
                    assigneeUsername,
                    createdBy,
                    createdByUsername,
                    createdAt,
                    updatedAt,
                    comments.stream()
                            .map(comment -> new TicketCommentResponse(
                                    comment.id(),
                                    comment.ticketId(),
                                    comment.content(),
                                    comment.createdBy(),
                                    comment.createdByUsername(),
                                    comment.createdAt()
                            ))
                            .toList(),
                    operationLogs.stream()
                            .map(log -> new TicketOperationLogResponse(
                                    log.id(),
                                    log.operationType(),
                                    log.detail(),
                                    log.operatorId(),
                                    log.operatorUsername(),
                                    log.createdAt()
                            ))
                            .toList()
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
