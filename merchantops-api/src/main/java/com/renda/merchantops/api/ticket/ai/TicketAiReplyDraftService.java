package com.renda.merchantops.api.ticket.ai;

import com.renda.merchantops.api.ai.core.AiProviderException;
import com.renda.merchantops.api.ai.core.AiProviderFailureType;
import com.renda.merchantops.api.ai.ticket.replydraft.TicketReplyDraftAiProvider;
import com.renda.merchantops.api.ai.ticket.replydraft.TicketReplyDraftPrompt;
import com.renda.merchantops.api.ai.ticket.replydraft.TicketReplyDraftPromptBuilder;
import com.renda.merchantops.api.ai.ticket.replydraft.TicketReplyDraftProviderRequest;
import com.renda.merchantops.api.ai.ticket.replydraft.TicketReplyDraftProviderResult;
import com.renda.merchantops.api.config.AiProperties;
import com.renda.merchantops.api.dto.ticket.query.TicketAiReplyDraftResponse;
import com.renda.merchantops.api.ticket.ai.TicketAiExecutionSupport;
import com.renda.merchantops.domain.ticket.TicketPromptContext;
import com.renda.merchantops.domain.ticket.TicketQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TicketAiReplyDraftService {

    private static final String INTERACTION_TYPE_REPLY_DRAFT = "REPLY_DRAFT";
    private static final String NEXT_STEP_LABEL = "Next step: ";
    private static final int MAX_COMMENT_LENGTH = 2000;

    private final TicketQueryUseCase ticketQueryUseCase;
    private final TicketReplyDraftPromptBuilder ticketReplyDraftPromptBuilder;
    private final TicketReplyDraftAiProvider ticketReplyDraftAiProvider;
    private final TicketAiExecutionSupport ticketAiExecutionSupport;
    private final AiProperties aiProperties;

    public TicketAiReplyDraftResponse generateReplyDraft(Long tenantId, Long userId, String requestId, Long ticketId) {
        String normalizedRequestId = ticketAiExecutionSupport.normalizeRequestId(requestId);
        TicketPromptContext ticket = ticketQueryUseCase.getTicketPromptContext(tenantId, ticketId);
        String promptVersion = ticketAiExecutionSupport.normalizePromptVersion(aiProperties.getReplyDraftPromptVersion(), "ticket-reply-draft-v1");
        String configuredModelId = ticketAiExecutionSupport.normalizeNullable(aiProperties.resolveModelId());
        TicketReplyDraftPrompt prompt = ticketReplyDraftPromptBuilder.build(promptVersion, ticket);

        ticketAiExecutionSupport.assertAvailable(
                tenantId,
                userId,
                normalizedRequestId,
                ticketId,
                INTERACTION_TYPE_REPLY_DRAFT,
                promptVersion,
                configuredModelId,
                aiProperties,
                "ticket ai reply draft is disabled",
                "ticket ai reply draft is unavailable"
        );

        long startedAt = System.nanoTime();
        try {
            TicketReplyDraftProviderResult providerResult = ticketReplyDraftAiProvider.generateReplyDraft(
                    new TicketReplyDraftProviderRequest(
                            normalizedRequestId,
                            ticketId,
                            configuredModelId,
                            aiProperties.getTimeoutMs(),
                            prompt
                    )
            );
            long latencyMs = ticketAiExecutionSupport.elapsedMillis(startedAt);
            String resolvedModelId = ticketAiExecutionSupport.normalizeNullable(providerResult.modelId());
            String opening = normalizeRequiredSection(providerResult.opening(), "opening");
            String body = normalizeRequiredSection(providerResult.body(), "body");
            String nextStep = normalizeRequiredSection(providerResult.nextStep(), "nextStep");
            String closing = normalizeRequiredSection(providerResult.closing(), "closing");
            String draftText = assembleDraft(opening, body, nextStep, closing);

            ticketAiExecutionSupport.recordSuccess(
                    tenantId,
                    userId,
                    normalizedRequestId,
                    ticketId,
                    INTERACTION_TYPE_REPLY_DRAFT,
                    promptVersion,
                    resolvedModelId,
                    latencyMs,
                    "nextStep=" + nextStep,
                    providerResult
            );

            return new TicketAiReplyDraftResponse(
                    ticketId,
                    draftText,
                    opening,
                    body,
                    nextStep,
                    closing,
                    promptVersion,
                    resolvedModelId,
                    LocalDateTime.now(),
                    latencyMs,
                    normalizedRequestId
            );
        } catch (AiProviderException ex) {
            long latencyMs = ticketAiExecutionSupport.elapsedMillis(startedAt);
            ticketAiExecutionSupport.recordFailure(
                    tenantId,
                    userId,
                    normalizedRequestId,
                    ticketId,
                    INTERACTION_TYPE_REPLY_DRAFT,
                    promptVersion,
                    configuredModelId,
                    ex.getFailureType(),
                    latencyMs
            );
            throw ticketAiExecutionSupport.toBizException(ex, "ticket ai reply draft timed out", "ticket ai reply draft is unavailable");
        }
    }

    private String normalizeRequiredSection(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider reply draft payload is missing " + fieldName);
        }
        return value.trim();
    }

    private String assembleDraft(String opening, String body, String nextStep, String closing) {
        // Keep suggestion-only drafts inside the current ticket comment limit before returning them to callers.
        String draftText = opening + "\n\n" + body + "\n\n" + NEXT_STEP_LABEL + nextStep + "\n\n" + closing;
        if (draftText.length() > MAX_COMMENT_LENGTH) {
            throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider reply draft exceeds comment length limit");
        }
        return draftText;
    }
}
