package com.renda.merchantops.api.ai.eval;

import com.fasterxml.jackson.core.type.TypeReference;
import com.renda.merchantops.api.ai.core.AiInteractionExecutionSupport;
import com.renda.merchantops.api.ai.support.StubStructuredOutputAiClient;
import com.renda.merchantops.api.ai.ticket.replydraft.OpenAiTicketReplyDraftProvider;
import com.renda.merchantops.api.ai.ticket.replydraft.TicketReplyDraftAiProvider;
import com.renda.merchantops.api.ai.ticket.replydraft.TicketReplyDraftPromptBuilder;
import com.renda.merchantops.api.ai.ticket.replydraft.TicketReplyDraftProviderResult;
import com.renda.merchantops.api.ticket.ai.TicketAiReplyDraftService;
import com.renda.merchantops.domain.ai.AiInteractionRecordCommand;
import com.renda.merchantops.domain.ai.AiInteractionRecordUseCase;
import com.renda.merchantops.domain.ai.AiInteractionStatus;
import com.renda.merchantops.domain.shared.error.BizException;
import com.renda.merchantops.domain.shared.error.ErrorCode;
import com.renda.merchantops.domain.ticket.TicketPromptContext;
import com.renda.merchantops.domain.ticket.TicketQueryUseCase;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

final class TicketReplyDraftEvalRunner extends AbstractAiWorkflowEvalRunner {

    private static final long USER_ID = 7003L;

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
                runSample(failures, inventoryEntry, AiWorkflowEvalSampleType.GOLDEN, String.valueOf(sample.ticketId()),
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
        TicketQueryUseCase ticketQueryUseCase = mock(TicketQueryUseCase.class);
        AiInteractionRecordUseCase recordUseCase = mock(AiInteractionRecordUseCase.class);
        StubStructuredOutputAiClient structuredOutputAiClient = new StubStructuredOutputAiClient();
        structuredOutputAiClient.willReturn(loadStructuredOutputResponse(
                "/ai/ticket-reply-draft/provider-response-" + sample.ticketId() + ".json"
        ));
        when(ticketQueryUseCase.getTicketPromptContext(sample.tenantId(), sample.ticketId()))
                .thenReturn(sample.toPromptContext());

        TicketAiReplyDraftService service = new TicketAiReplyDraftService(
                ticketQueryUseCase,
                new TicketReplyDraftPromptBuilder(),
                new OpenAiTicketReplyDraftProvider(objectMapper, structuredOutputAiClient),
                new AiInteractionExecutionSupport(recordUseCase),
                aiProperties(inventoryEntry.workflow())
        );

        var response = service.generateReplyDraft(
                sample.tenantId(),
                USER_ID,
                "golden-reply-draft-" + sample.ticketId(),
                sample.ticketId()
        );

        assertThat(response.ticketId()).isEqualTo(sample.ticketId());
        assertThat(response.opening()).isEqualTo(sample.expectedOpening());
        assertThat(response.body()).isEqualTo(sample.expectedBody());
        assertThat(response.nextStep()).isEqualTo(sample.expectedNextStep());
        assertThat(response.closing()).isEqualTo(sample.expectedClosing());
        assertThat(response.draftText()).isEqualTo(sample.expectedDraftText());
        assertThat(response.draftText()).contains("\n\nNext step: ");
        assertThat(response.draftText().length()).isLessThanOrEqualTo(2000);
        assertThat(response.promptVersion()).isEqualTo(inventoryEntry.expectedPromptVersion());
        assertThat(response.modelId()).isEqualTo("gpt-4.1-mini");
        assertThat(response.generatedAt()).isNotNull();
        assertThat(response.latencyMs()).isNotNegative();
        assertThat(response.requestId()).isEqualTo("golden-reply-draft-" + sample.ticketId());

        assertThat(structuredOutputAiClient.lastRequest().userPrompt()).contains(sample.title());
        assertThat(structuredOutputAiClient.lastRequest().userPrompt()).contains(sample.status());
        assertThat(structuredOutputAiClient.lastRequest().userPrompt()).contains(sample.operationLogs().getFirst().detail());

        ArgumentCaptor<AiInteractionRecordCommand> commandCaptor = ArgumentCaptor.forClass(AiInteractionRecordCommand.class);
        verify(recordUseCase).record(commandCaptor.capture());
        assertThat(commandCaptor.getValue().status()).isEqualTo(AiInteractionStatus.SUCCEEDED);
        assertThat(commandCaptor.getValue().interactionType()).isEqualTo(inventoryEntry.workflow().interactionType());
        assertThat(commandCaptor.getValue().promptVersion()).isEqualTo(inventoryEntry.expectedPromptVersion());
        assertThat(commandCaptor.getValue().outputSummary()).isEqualTo("nextStep=" + sample.expectedNextStep());
    }

    private void assertFailureSample(AiWorkflowEvalInventoryEntry inventoryEntry, FailureSample sample) {
        TicketQueryUseCase ticketQueryUseCase = mock(TicketQueryUseCase.class);
        AiInteractionRecordUseCase recordUseCase = mock(AiInteractionRecordUseCase.class);
        TicketReplyDraftAiProvider ticketReplyDraftAiProvider = mock(TicketReplyDraftAiProvider.class);
        when(ticketQueryUseCase.getTicketPromptContext(sample.promptContext().tenantId(), sample.promptContext().id()))
                .thenReturn(sample.promptContext());
        when(ticketReplyDraftAiProvider.generateReplyDraft(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new TicketReplyDraftProviderResult(
                        sample.opening(),
                        sample.body(),
                        sample.nextStep(),
                        sample.closing(),
                        "gpt-4.1-mini",
                        120,
                        44,
                        164,
                        null
                ));

        TicketAiReplyDraftService service = new TicketAiReplyDraftService(
                ticketQueryUseCase,
                new TicketReplyDraftPromptBuilder(),
                ticketReplyDraftAiProvider,
                new AiInteractionExecutionSupport(recordUseCase),
                aiProperties(inventoryEntry.workflow())
        );

        assertThatThrownBy(() -> service.generateReplyDraft(
                sample.promptContext().tenantId(),
                USER_ID,
                "failure-reply-draft-" + sample.id(),
                sample.promptContext().id()
        )).isInstanceOfSatisfying(BizException.class, ex -> {
            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SERVICE_UNAVAILABLE);
            assertThat(ex.getMessage()).isEqualTo(sample.expectedMessage());
        });

        ArgumentCaptor<AiInteractionRecordCommand> commandCaptor = ArgumentCaptor.forClass(AiInteractionRecordCommand.class);
        verify(recordUseCase).record(commandCaptor.capture());
        assertThat(commandCaptor.getValue().status()).isEqualTo(AiInteractionStatus.valueOf(sample.expectedStatus()));
    }

    private void assertPolicySample(AiWorkflowEvalInventoryEntry inventoryEntry, PolicySample sample) {
        TicketQueryUseCase ticketQueryUseCase = mock(TicketQueryUseCase.class);
        AiInteractionRecordUseCase recordUseCase = mock(AiInteractionRecordUseCase.class);
        TicketReplyDraftAiProvider ticketReplyDraftAiProvider = mock(TicketReplyDraftAiProvider.class);
        when(ticketQueryUseCase.getTicketPromptContext(sample.promptContext().tenantId(), sample.promptContext().id()))
                .thenReturn(sample.promptContext());
        when(ticketReplyDraftAiProvider.generateReplyDraft(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new TicketReplyDraftProviderResult(
                        sample.opening(),
                        sample.body(),
                        sample.nextStep(),
                        sample.closing(),
                        "gpt-4.1-mini",
                        120,
                        44,
                        164,
                        null
                ));

        TicketAiReplyDraftService service = new TicketAiReplyDraftService(
                ticketQueryUseCase,
                new TicketReplyDraftPromptBuilder(),
                ticketReplyDraftAiProvider,
                new AiInteractionExecutionSupport(recordUseCase),
                aiProperties(inventoryEntry.workflow())
        );

        var response = service.generateReplyDraft(
                sample.promptContext().tenantId(),
                USER_ID,
                "policy-reply-draft-" + sample.id(),
                sample.promptContext().id()
        );

        assertThat(response.draftText()).contains(sample.mustContain().toArray(String[]::new));
        assertThat(response.draftText()).doesNotContain(sample.mustNotContain().toArray(String[]::new));
        assertThat(response.draftText().length()).isLessThanOrEqualTo(2000);

        ArgumentCaptor<AiInteractionRecordCommand> commandCaptor = ArgumentCaptor.forClass(AiInteractionRecordCommand.class);
        verify(recordUseCase).record(commandCaptor.capture());
        assertThat(commandCaptor.getValue().status()).isEqualTo(AiInteractionStatus.SUCCEEDED);
        assertThat(commandCaptor.getValue().outputSummary()).isEqualTo("nextStep=" + sample.nextStep());
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
        private TicketPromptContext toPromptContext() {
            return new TicketPromptContext(
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
                            .map(comment -> new TicketPromptContext.Comment(
                                    comment.id(),
                                    comment.content(),
                                    comment.createdByUsername(),
                                    comment.createdAt()
                            ))
                            .toList(),
                    false,
                    operationLogs.stream()
                            .map(log -> new TicketPromptContext.OperationLog(
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

    private record FailureSample(
            String id,
            TicketPromptContext promptContext,
            String opening,
            String body,
            String nextStep,
            String closing,
            String expectedStatus,
            String expectedMessage
    ) {
    }

    private record PolicySample(
            String id,
            TicketPromptContext promptContext,
            String opening,
            String body,
            String nextStep,
            String closing,
            List<String> mustContain,
            List<String> mustNotContain
    ) {
    }
}
