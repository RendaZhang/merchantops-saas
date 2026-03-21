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

class TicketReplyDraftGoldenSampleTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void goldenSamplesShouldKeepStableStubReplyDraftFormat() throws Exception {
        List<GoldenSample> samples = loadSamples();
        TicketReplyDraftPromptBuilder promptBuilder = new TicketReplyDraftPromptBuilder();
        TicketReplyDraftAiProvider stubProvider = request -> samples.stream()
                .filter(sample -> sample.ticketId().equals(request.ticketId()))
                .findFirst()
                .map(sample -> new TicketReplyDraftProviderResult(
                        sample.expectedOpening(),
                        sample.expectedBody(),
                        sample.expectedNextStep(),
                        sample.expectedClosing(),
                        "stub-reply-draft-v1",
                        null,
                        null,
                        null,
                        null
                ))
                .orElseThrow();

        for (GoldenSample sample : samples) {
            TicketDetailResponse ticket = sample.toTicketDetailResponse();
            TicketReplyDraftPrompt prompt = promptBuilder.build("ticket-reply-draft-v1", ticket);
            TicketReplyDraftProviderResult result = stubProvider.generateReplyDraft(
                    new TicketReplyDraftProviderRequest("golden-" + sample.ticketId(), sample.ticketId(), "stub-reply-draft-v1", 1000, prompt)
            );
            String draftText = assembleDraft(result);

            assertThat(prompt.userPrompt()).contains(sample.title());
            assertThat(prompt.userPrompt()).contains(sample.status());
            assertThat(prompt.userPrompt()).contains(sample.operationLogs().getFirst().detail());
            assertThat(result.opening()).isEqualTo(sample.expectedOpening());
            assertThat(result.body()).isEqualTo(sample.expectedBody());
            assertThat(result.nextStep()).isEqualTo(sample.expectedNextStep());
            assertThat(result.closing()).isEqualTo(sample.expectedClosing());
            assertThat(draftText).isEqualTo(sample.expectedDraftText());
            assertThat(draftText).contains("\n\nNext step: ");
            assertThat(draftText.length()).isLessThanOrEqualTo(2000);
        }
    }

    private List<GoldenSample> loadSamples() throws Exception {
        try (InputStream inputStream = getClass().getResourceAsStream("/ai/ticket-reply-draft/golden-samples.json")) {
            assertThat(inputStream).isNotNull();
            return objectMapper.readValue(inputStream, new TypeReference<>() {
            });
        }
    }

    private String assembleDraft(TicketReplyDraftProviderResult result) {
        return result.opening() + "\n\n" + result.body() + "\n\nNext step: " + result.nextStep() + "\n\n" + result.closing();
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
            String expectedOpening,
            String expectedBody,
            String expectedNextStep,
            String expectedClosing,
            String expectedDraftText
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
