package com.renda.merchantops.api.service;

import com.renda.merchantops.api.ai.AiInteractionRecordCommand;
import com.renda.merchantops.api.ai.AiInteractionRecordService;
import com.renda.merchantops.api.ai.AiInteractionStatus;
import com.renda.merchantops.api.ai.AiProviderException;
import com.renda.merchantops.api.ai.AiProviderFailureType;
import com.renda.merchantops.api.ai.TicketAiPromptContext;
import com.renda.merchantops.api.ai.TicketTriageAiProvider;
import com.renda.merchantops.api.ai.TicketTriagePrompt;
import com.renda.merchantops.api.ai.TicketTriagePromptBuilder;
import com.renda.merchantops.api.ai.TicketTriageProviderRequest;
import com.renda.merchantops.api.ai.TicketTriageProviderResult;
import com.renda.merchantops.api.config.AiProperties;
import com.renda.merchantops.api.context.RequestIdPolicy;
import com.renda.merchantops.api.dto.ticket.query.TicketAiTriagePriority;
import com.renda.merchantops.api.dto.ticket.query.TicketAiTriageResponse;
import com.renda.merchantops.common.exception.BizException;
import com.renda.merchantops.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TicketAiTriageService {

    private static final String ENTITY_TYPE_TICKET = "TICKET";
    private static final String INTERACTION_TYPE_TRIAGE = "TRIAGE";

    private final TicketQueryService ticketQueryService;
    private final TicketTriagePromptBuilder ticketTriagePromptBuilder;
    private final TicketTriageAiProvider ticketTriageAiProvider;
    private final AiInteractionRecordService aiInteractionRecordService;
    private final AiProperties aiProperties;

    public TicketAiTriageResponse generateTriage(Long tenantId, Long userId, String requestId, Long ticketId) {
        String normalizedRequestId = RequestIdPolicy.requireNormalized(requestId);
        TicketAiPromptContext ticket = ticketQueryService.getTicketPromptContext(tenantId, ticketId);
        String promptVersion = normalizePromptVersion(aiProperties.getTriagePromptVersion());
        String configuredModelId = normalizeNullable(aiProperties.resolveModelId());
        TicketTriagePrompt prompt = ticketTriagePromptBuilder.build(promptVersion, ticket);

        if (!aiProperties.isEnabled()) {
            recordInteraction(tenantId, userId, normalizedRequestId, ticketId, promptVersion, configuredModelId, AiInteractionStatus.FEATURE_DISABLED, 0L, null, null);
            throw new BizException(ErrorCode.SERVICE_UNAVAILABLE, "ticket ai triage is disabled");
        }
        if (!aiProperties.hasProviderConfiguration()) {
            recordInteraction(tenantId, userId, normalizedRequestId, ticketId, promptVersion, configuredModelId, AiInteractionStatus.PROVIDER_NOT_CONFIGURED, 0L, null, null);
            throw new BizException(ErrorCode.SERVICE_UNAVAILABLE, "ticket ai triage is unavailable");
        }

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
            long latencyMs = elapsedMillis(startedAt);
            String resolvedModelId = normalizeNullable(providerResult.modelId());
            String classification = normalizeRequiredText(providerResult.classification());
            TicketAiTriagePriority priority = normalizeRequiredPriority(providerResult.priority());
            String reasoning = normalizeRequiredText(providerResult.reasoning());

            recordInteraction(
                    tenantId,
                    userId,
                    normalizedRequestId,
                    ticketId,
                    promptVersion,
                    resolvedModelId,
                    AiInteractionStatus.SUCCEEDED,
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
            long latencyMs = elapsedMillis(startedAt);
            AiInteractionStatus status = mapFailure(ex.getFailureType());
            recordInteraction(tenantId, userId, normalizedRequestId, ticketId, promptVersion, configuredModelId, status, latencyMs, null, null);
            throw toBizException(ex);
        }
    }

    private void recordInteraction(Long tenantId,
                                   Long userId,
                                   String requestId,
                                   Long ticketId,
                                   String promptVersion,
                                   String modelId,
                                   AiInteractionStatus status,
                                   long latencyMs,
                                   String outputSummary,
                                   TicketTriageProviderResult providerResult) {
        aiInteractionRecordService.record(new AiInteractionRecordCommand(
                tenantId,
                userId,
                requestId,
                ENTITY_TYPE_TICKET,
                ticketId,
                INTERACTION_TYPE_TRIAGE,
                promptVersion,
                modelId,
                status,
                latencyMs,
                outputSummary,
                providerResult == null ? null : providerResult.inputTokens(),
                providerResult == null ? null : providerResult.outputTokens(),
                providerResult == null ? null : providerResult.totalTokens(),
                providerResult == null ? null : providerResult.costMicros()
        ));
    }

    private AiInteractionStatus mapFailure(AiProviderFailureType failureType) {
        return switch (failureType) {
            case TIMEOUT -> AiInteractionStatus.PROVIDER_TIMEOUT;
            case INVALID_RESPONSE -> AiInteractionStatus.INVALID_RESPONSE;
            case UNAVAILABLE -> AiInteractionStatus.PROVIDER_UNAVAILABLE;
        };
    }

    private BizException toBizException(AiProviderException ex) {
        if (ex.getFailureType() == AiProviderFailureType.TIMEOUT) {
            return new BizException(ErrorCode.SERVICE_UNAVAILABLE, "ticket ai triage timed out");
        }
        return new BizException(ErrorCode.SERVICE_UNAVAILABLE, "ticket ai triage is unavailable");
    }

    private long elapsedMillis(long startedAt) {
        return Math.max(0L, (System.nanoTime() - startedAt) / 1_000_000L);
    }

    private String normalizePromptVersion(String value) {
        if (!StringUtils.hasText(value)) {
            return "ticket-triage-v1";
        }
        return value.trim();
    }

    private String normalizeNullable(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
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
