package com.renda.merchantops.api.ticket.ai;

import com.renda.merchantops.api.ai.core.AiProviderException;
import com.renda.merchantops.api.ai.core.AiProviderFailureType;
import com.renda.merchantops.api.ai.core.AiUsageAwareResult;
import com.renda.merchantops.api.config.AiProperties;
import com.renda.merchantops.domain.ai.AiInteractionRecordCommand;
import com.renda.merchantops.domain.ai.AiInteractionRecordUseCase;
import com.renda.merchantops.domain.ai.AiInteractionStatus;
import com.renda.merchantops.api.context.RequestIdPolicy;
import com.renda.merchantops.domain.shared.error.BizException;
import com.renda.merchantops.domain.shared.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Keeps the ticket AI service flows explicit while centralizing the shared
 * request normalization, degraded-mode recording, and provider failure mapping.
 */
@Component
@RequiredArgsConstructor
public class TicketAiExecutionSupport {

    private static final String ENTITY_TYPE_TICKET = "TICKET";

    private final AiInteractionRecordUseCase aiInteractionRecordUseCase;

    public String normalizeRequestId(String requestId) {
        return RequestIdPolicy.requireNormalized(requestId);
    }

    public String normalizePromptVersion(String value, String fallback) {
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        return value.trim();
    }

    public String normalizeNullable(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    public void assertAvailable(Long tenantId,
                                Long userId,
                                String requestId,
                                Long ticketId,
                                String interactionType,
                                String promptVersion,
                                String modelId,
                                AiProperties aiProperties,
                                String disabledMessage,
                                String unavailableMessage) {
        if (!aiProperties.isEnabled()) {
            record(tenantId, userId, requestId, ticketId, interactionType, promptVersion, modelId,
                    AiInteractionStatus.FEATURE_DISABLED, 0L, null, null);
            throw new BizException(ErrorCode.SERVICE_UNAVAILABLE, disabledMessage);
        }
        if (!aiProperties.hasProviderConfiguration()) {
            record(tenantId, userId, requestId, ticketId, interactionType, promptVersion, modelId,
                    AiInteractionStatus.PROVIDER_NOT_CONFIGURED, 0L, null, null);
            throw new BizException(ErrorCode.SERVICE_UNAVAILABLE, unavailableMessage);
        }
    }

    public long elapsedMillis(long startedAt) {
        return Math.max(0L, (System.nanoTime() - startedAt) / 1_000_000L);
    }

    public AiInteractionStatus mapFailure(AiProviderFailureType failureType) {
        return switch (failureType) {
            case TIMEOUT -> AiInteractionStatus.PROVIDER_TIMEOUT;
            case INVALID_RESPONSE -> AiInteractionStatus.INVALID_RESPONSE;
            case UNAVAILABLE -> AiInteractionStatus.PROVIDER_UNAVAILABLE;
        };
    }

    public BizException toBizException(AiProviderException ex, String timeoutMessage, String unavailableMessage) {
        if (ex.getFailureType() == AiProviderFailureType.TIMEOUT) {
            return new BizException(ErrorCode.SERVICE_UNAVAILABLE, timeoutMessage);
        }
        return new BizException(ErrorCode.SERVICE_UNAVAILABLE, unavailableMessage);
    }

    public void recordSuccess(Long tenantId,
                              Long userId,
                              String requestId,
                              Long ticketId,
                              String interactionType,
                              String promptVersion,
                              String modelId,
                              long latencyMs,
                              String outputSummary,
                              AiUsageAwareResult usageResult) {
        record(tenantId, userId, requestId, ticketId, interactionType, promptVersion, modelId,
                AiInteractionStatus.SUCCEEDED, latencyMs, outputSummary, usageResult);
    }

    public void recordFailure(Long tenantId,
                              Long userId,
                              String requestId,
                              Long ticketId,
                              String interactionType,
                              String promptVersion,
                              String modelId,
                              AiProviderFailureType failureType,
                              long latencyMs) {
        record(tenantId, userId, requestId, ticketId, interactionType, promptVersion, modelId,
                mapFailure(failureType), latencyMs, null, null);
    }

    private void record(Long tenantId,
                        Long userId,
                        String requestId,
                        Long ticketId,
                        String interactionType,
                        String promptVersion,
                        String modelId,
                        AiInteractionStatus status,
                        long latencyMs,
                        String outputSummary,
                        AiUsageAwareResult usageResult) {
        aiInteractionRecordUseCase.record(new AiInteractionRecordCommand(
                tenantId,
                userId,
                requestId,
                ENTITY_TYPE_TICKET,
                ticketId,
                interactionType,
                promptVersion,
                modelId,
                status,
                latencyMs,
                outputSummary,
                usageResult == null ? null : usageResult.inputTokens(),
                usageResult == null ? null : usageResult.outputTokens(),
                usageResult == null ? null : usageResult.totalTokens(),
                usageResult == null ? null : usageResult.costMicros()
        ));
    }
}
