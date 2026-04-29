package com.renda.merchantops.api.ticket.ai;

import com.renda.merchantops.api.ai.core.AiProviderException;
import com.renda.merchantops.api.ai.core.AiProviderFailureType;
import com.renda.merchantops.api.ai.core.AiGenerationWorkflow;
import com.renda.merchantops.api.ai.core.AiInteractionExecutionSupport;
import com.renda.merchantops.api.ai.ticket.summary.TicketSummaryAiProvider;
import com.renda.merchantops.api.ai.ticket.summary.TicketSummaryPrompt;
import com.renda.merchantops.api.ai.ticket.summary.TicketSummaryPromptBuilder;
import com.renda.merchantops.api.ai.ticket.summary.TicketSummaryProviderRequest;
import com.renda.merchantops.api.ai.ticket.summary.TicketSummaryProviderResult;
import com.renda.merchantops.api.config.AiProperties;
import com.renda.merchantops.api.dto.ticket.query.TicketAiSummaryResponse;
import com.renda.merchantops.api.featureflag.FeatureFlagGateService;
import com.renda.merchantops.domain.featureflag.FeatureFlagKey;
import com.renda.merchantops.domain.ticket.TicketPromptContext;
import com.renda.merchantops.domain.ticket.TicketQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TicketAiSummaryService {

    private static final AiGenerationWorkflow WORKFLOW = AiGenerationWorkflow.TICKET_SUMMARY;

    private final TicketQueryUseCase ticketQueryUseCase;
    private final TicketSummaryPromptBuilder ticketSummaryPromptBuilder;
    private final TicketSummaryAiProvider ticketSummaryAiProvider;
    private final AiInteractionExecutionSupport aiInteractionExecutionSupport;
    private final FeatureFlagGateService featureFlagGateService;
    private final AiProperties aiProperties;

    public TicketAiSummaryResponse generateSummary(Long tenantId, Long userId, String requestId, Long ticketId) {
        String normalizedRequestId = aiInteractionExecutionSupport.normalizeRequestId(requestId);
        TicketPromptContext ticket = ticketQueryUseCase.getTicketPromptContext(tenantId, ticketId);
        String promptVersion = WORKFLOW.resolvePromptVersion(aiProperties, aiInteractionExecutionSupport);
        String configuredModelId = aiInteractionExecutionSupport.normalizeNullable(aiProperties.resolveModelId());
        TicketSummaryPrompt prompt = ticketSummaryPromptBuilder.build(promptVersion, ticket);

        aiInteractionExecutionSupport.assertAvailable(
                tenantId,
                userId,
                normalizedRequestId,
                WORKFLOW.entityType(),
                ticketId,
                WORKFLOW.interactionType(),
                promptVersion,
                configuredModelId,
                aiProperties,
                featureFlagGateService.isEnabled(tenantId, FeatureFlagKey.AI_TICKET_SUMMARY),
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
            long latencyMs = aiInteractionExecutionSupport.elapsedMillis(startedAt);
            String resolvedModelId = aiInteractionExecutionSupport.normalizeNullable(providerResult.modelId());
            String summary = normalizeRequiredSummary(providerResult.summary());

            aiInteractionExecutionSupport.recordSuccess(
                    tenantId,
                    userId,
                    normalizedRequestId,
                    WORKFLOW.entityType(),
                    ticketId,
                    WORKFLOW.interactionType(),
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
            long latencyMs = aiInteractionExecutionSupport.elapsedMillis(startedAt);
            aiInteractionExecutionSupport.recordFailure(
                    tenantId,
                    userId,
                    normalizedRequestId,
                    WORKFLOW.entityType(),
                    ticketId,
                    WORKFLOW.interactionType(),
                    promptVersion,
                    configuredModelId,
                    ex.getFailureType(),
                    latencyMs
            );
            throw aiInteractionExecutionSupport.toBizException(ex, "ticket ai summary timed out", "ticket ai summary is unavailable");
        }
    }

    private String normalizeRequiredSummary(String value) {
        if (!StringUtils.hasText(value)) {
            throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider summary payload is blank");
        }
        return value.trim();
    }
}
