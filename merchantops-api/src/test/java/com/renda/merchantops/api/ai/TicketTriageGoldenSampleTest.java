package com.renda.merchantops.api.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.renda.merchantops.api.dto.ticket.query.TicketAiTriagePriority;
import com.renda.merchantops.api.dto.ticket.query.TicketCommentResponse;
import com.renda.merchantops.api.dto.ticket.query.TicketDetailResponse;
import com.renda.merchantops.api.dto.ticket.query.TicketOperationLogResponse;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TicketTriageGoldenSampleTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void goldenSamplesShouldKeepStableStubTriageFormat() throws Exception {
        List<GoldenSample> samples = loadSamples();
        TicketTriagePromptBuilder promptBuilder = new TicketTriagePromptBuilder();
        TicketTriageAiProvider stubProvider = request -> samples.stream()
                .filter(sample -> sample.ticketId().equals(request.ticketId()))
                .findFirst()
                .map(sample -> new TicketTriageProviderResult(
                        sample.expectedClassification(),
                        sample.expectedPriority(),
                        sample.expectedReasoning(),
                        "stub-triage-v1",
                        null,
                        null,
                        null,
                        null
                ))
                .orElseThrow();

        for (GoldenSample sample : samples) {
            TicketDetailResponse ticket = sample.toTicketDetailResponse();
            TicketTriagePrompt prompt = promptBuilder.build("ticket-triage-v1", ticket);
            TicketTriageProviderResult result = stubProvider.generateTriage(
                    new TicketTriageProviderRequest("golden-" + sample.ticketId(), sample.ticketId(), "stub-triage-v1", 1000, prompt)
            );

            assertThat(prompt.userPrompt()).contains(sample.title());
            assertThat(prompt.userPrompt()).contains(sample.status());
            assertThat(prompt.userPrompt()).contains(sample.operationLogs().getFirst().detail());
            assertThat(result.classification()).isEqualTo(sample.expectedClassification());
            assertThat(result.priority()).isEqualTo(sample.expectedPriority());
            assertThat(result.reasoning()).isEqualTo(sample.expectedReasoning());
            assertThat(result.priority()).isIn(TicketAiTriagePriority.LOW, TicketAiTriagePriority.MEDIUM, TicketAiTriagePriority.HIGH);
            assertThat(result.reasoning()).isNotBlank();
        }
    }

    private List<GoldenSample> loadSamples() throws Exception {
        try (InputStream inputStream = getClass().getResourceAsStream("/ai/ticket-triage/golden-samples.json")) {
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
            String expectedClassification,
            TicketAiTriagePriority expectedPriority,
            String expectedReasoning
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
