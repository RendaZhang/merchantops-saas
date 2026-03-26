package com.renda.merchantops.domain.ai;

import com.renda.merchantops.domain.shared.error.BizException;
import com.renda.merchantops.domain.shared.error.ErrorCode;

import java.time.LocalDateTime;
import java.util.Locale;

public class AiInteractionRecordDomainService implements AiInteractionRecordUseCase {

    private final AiInteractionRecordPort aiInteractionRecordPort;

    public AiInteractionRecordDomainService(AiInteractionRecordPort aiInteractionRecordPort) {
        this.aiInteractionRecordPort = aiInteractionRecordPort;
    }

    @Override
    public void record(AiInteractionRecordCommand command) {
        if (command == null
                || command.tenantId() == null
                || command.userId() == null
                || command.entityId() == null
                || command.status() == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "ai interaction context missing");
        }
        if (!hasText(command.requestId())
                || !hasText(command.entityType())
                || !hasText(command.interactionType())
                || !hasText(command.promptVersion())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "ai interaction required fields missing");
        }

        aiInteractionRecordPort.save(new NewAiInteractionRecord(
                command.tenantId(),
                command.userId(),
                command.requestId().trim(),
                normalizeKey(command.entityType()),
                command.entityId(),
                normalizeKey(command.interactionType()),
                command.promptVersion().trim(),
                normalizeNullable(command.modelId()),
                command.status().name(),
                command.latencyMs(),
                normalizeNullable(command.outputSummary()),
                command.usagePromptTokens(),
                command.usageCompletionTokens(),
                command.usageTotalTokens(),
                command.usageCostMicros(),
                LocalDateTime.now()
        ));
    }

    private String normalizeKey(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeNullable(String value) {
        if (!hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
