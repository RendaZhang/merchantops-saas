package com.renda.merchantops.api.ticket.ai;

import com.renda.merchantops.api.ai.core.AiProviderException;
import com.renda.merchantops.api.ai.core.AiProviderFailureType;
import com.renda.merchantops.api.ai.ticket.summary.TicketSummaryAiProvider;
import com.renda.merchantops.api.ai.ticket.summary.TicketSummaryPrompt;
import com.renda.merchantops.api.ai.ticket.summary.TicketSummaryPromptBuilder;
import com.renda.merchantops.api.ai.ticket.summary.TicketSummaryProviderRequest;
import com.renda.merchantops.api.ai.ticket.summary.TicketSummaryProviderResult;
import com.renda.merchantops.api.config.AiProperties;
import com.renda.merchantops.api.dto.ticket.query.TicketAiSummaryResponse;
import com.renda.merchantops.api.ticket.ai.TicketAiExecutionSupport;
import com.renda.merchantops.domain.ticket.TicketPromptContext;
import com.renda.merchantops.domain.ticket.TicketQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TicketAiSummaryService {

    private static final String INTERACTION_TYPE_SUMMARY = "SUMMARY";

    private final TicketQueryUseCase ticketQueryUseCase;
    private final TicketSummaryPromptBuilder ticketSummaryPromptBuilder;
    private final TicketSummaryAiProvider ticketSummaryAiProvider;
    private final TicketAiExecutionSupport ticketAiExecutionSupport;
    private final AiProperties aiProperties;

    public TicketAiSummaryResponse generateSummary(Long tenantId, Long userId, String requestId, Long ticketId) {
        String normalizedRequestId = ticketAiExecutionSupport.normalizeRequestId(requestId);
        TicketPromptContext ticket = ticketQueryUseCase.getTicketPromptContext(tenantId, ticketId);
        String promptVersion = ticketAiExecutionSupport.normalizePromptVersion(aiProperties.getPromptVersion(), "ticket-summary-v1");
        String configuredModelId = ticketAiExecutionSupport.normalizeNullable(aiProperties.resolveModelId());
        TicketSummaryPrompt prompt = ticketSummaryPromptBuilder.build(promptVersion, ticket);

        ticketAiExecutionSupport.assertAvailable(
                tenantId,
                userId,
                normalizedRequestId,
                ticketId,
                INTERACTION_TYPE_SUMMARY,
                promptVersion,
                configuredModelId,
                aiProperties,
                "ticket ai summary is disabled",
                "ticket ai summary is unavailable"
        );

        long startedAt = System.nanoTime();
        try {
            TicketSummaryProviderResult providerResult = ticketSummaryAiProvider.generateSummary(
                    new TicketSummaryProviderRequest(
                            normalizedRequestId,
                            ticketId,
                            configuredModelId,
                            aiProperties.getTimeoutMs(),
                            prompt
                    )
            );
            long latencyMs = ticketAiExecutionSupport.elapsedMillis(startedAt);
            String resolvedModelId = ticketAiExecutionSupport.normalizeNullable(providerResult.modelId());
            String summary = normalizeRequiredSummary(providerResult.summary());

            ticketAiExecutionSupport.recordSuccess(
                    tenantId,
                    userId,
                    normalizedRequestId,
                    ticketId,
                    INTERACTION_TYPE_SUMMARY,
                    promptVersion,
                    resolvedModelId,
                    latencyMs,
                    summary,
                    providerResult
            );

            return new TicketAiSummaryResponse(
                    ticketId,
                    summary,
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
                    INTERACTION_TYPE_SUMMARY,
                    promptVersion,
                    configuredModelId,
                    ex.getFailureType(),
                    latencyMs
            );
            throw ticketAiExecutionSupport.toBizException(ex, "ticket ai summary timed out", "ticket ai summary is unavailable");
        }
    }

    private String normalizeRequiredSummary(String value) {
        if (!StringUtils.hasText(value)) {
            throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider summary payload is blank");
        }
        return value.trim();
    }
}
