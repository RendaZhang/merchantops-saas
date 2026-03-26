package com.renda.merchantops.api.ticket.ai;

import com.renda.merchantops.api.ai.core.AiProviderException;
import com.renda.merchantops.api.ai.core.AiProviderFailureType;
import com.renda.merchantops.api.ai.ticket.triage.TicketTriageAiProvider;
import com.renda.merchantops.api.ai.ticket.triage.TicketTriagePrompt;
import com.renda.merchantops.api.ai.ticket.triage.TicketTriagePromptBuilder;
import com.renda.merchantops.api.ai.ticket.triage.TicketTriageProviderRequest;
import com.renda.merchantops.api.ai.ticket.triage.TicketTriageProviderResult;
import com.renda.merchantops.api.config.AiProperties;
import com.renda.merchantops.api.dto.ticket.query.TicketAiTriagePriority;
import com.renda.merchantops.api.dto.ticket.query.TicketAiTriageResponse;
import com.renda.merchantops.api.ticket.ai.TicketAiExecutionSupport;
import com.renda.merchantops.domain.ticket.TicketPromptContext;
import com.renda.merchantops.domain.ticket.TicketQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TicketAiTriageService {

    private static final String INTERACTION_TYPE_TRIAGE = "TRIAGE";

    private final TicketQueryUseCase ticketQueryUseCase;
    private final TicketTriagePromptBuilder ticketTriagePromptBuilder;
    private final TicketTriageAiProvider ticketTriageAiProvider;
    private final TicketAiExecutionSupport ticketAiExecutionSupport;
    private final AiProperties aiProperties;

    public TicketAiTriageResponse generateTriage(Long tenantId, Long userId, String requestId, Long ticketId) {
        String normalizedRequestId = ticketAiExecutionSupport.normalizeRequestId(requestId);
        TicketPromptContext ticket = ticketQueryUseCase.getTicketPromptContext(tenantId, ticketId);
        String promptVersion = ticketAiExecutionSupport.normalizePromptVersion(aiProperties.getTriagePromptVersion(), "ticket-triage-v1");
        String configuredModelId = ticketAiExecutionSupport.normalizeNullable(aiProperties.resolveModelId());
        TicketTriagePrompt prompt = ticketTriagePromptBuilder.build(promptVersion, ticket);

        ticketAiExecutionSupport.assertAvailable(
                tenantId,
                userId,
                normalizedRequestId,
                ticketId,
                INTERACTION_TYPE_TRIAGE,
                promptVersion,
                configuredModelId,
                aiProperties,
                "ticket ai triage is disabled",
                "ticket ai triage is unavailable"
        );

        long startedAt = System.nanoTime();
        try {
            TicketTriageProviderResult providerResult = ticketTriageAiProvider.generateTriage(
                    new TicketTriageProviderRequest(
                            normalizedRequestId,
                            ticketId,
                            configuredModelId,
                            aiProperties.getTimeoutMs(),
                            prompt
                    )
            );
            long latencyMs = ticketAiExecutionSupport.elapsedMillis(startedAt);
            String resolvedModelId = ticketAiExecutionSupport.normalizeNullable(providerResult.modelId());
            String classification = normalizeRequiredText(providerResult.classification());
            TicketAiTriagePriority priority = normalizeRequiredPriority(providerResult.priority());
            String reasoning = normalizeRequiredText(providerResult.reasoning());

            ticketAiExecutionSupport.recordSuccess(
                    tenantId,
                    userId,
                    normalizedRequestId,
                    ticketId,
                    INTERACTION_TYPE_TRIAGE,
                    promptVersion,
                    resolvedModelId,
                    latencyMs,
                    "classification=" + classification + "; priority=" + priority.name(),
                    providerResult
            );

            return new TicketAiTriageResponse(
                    ticketId,
                    classification,
                    priority,
                    reasoning,
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
                    INTERACTION_TYPE_TRIAGE,
                    promptVersion,
                    configuredModelId,
                    ex.getFailureType(),
                    latencyMs
            );
            throw ticketAiExecutionSupport.toBizException(ex, "ticket ai triage timed out", "ticket ai triage is unavailable");
        }
    }

    private String normalizeRequiredText(String value) {
        if (!StringUtils.hasText(value)) {
            throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider triage payload is missing required text");
        }
        return value.trim();
    }

    private TicketAiTriagePriority normalizeRequiredPriority(TicketAiTriagePriority value) {
        if (value == null) {
            throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider triage payload is missing priority");
        }
        return value;
    }
}
