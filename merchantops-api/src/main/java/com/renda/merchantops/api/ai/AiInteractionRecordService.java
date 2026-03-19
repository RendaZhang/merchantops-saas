package com.renda.merchantops.api.ai;

import com.renda.merchantops.api.context.RequestIdPolicy;
import com.renda.merchantops.common.exception.BizException;
import com.renda.merchantops.common.exception.ErrorCode;
import com.renda.merchantops.infra.persistence.entity.AiInteractionRecordEntity;
import com.renda.merchantops.infra.repository.AiInteractionRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AiInteractionRecordService {

    private final AiInteractionRecordRepository aiInteractionRecordRepository;

    @Transactional
    public void record(AiInteractionRecordCommand command) {
        if (command == null
                || command.tenantId() == null
                || command.userId() == null
                || command.entityId() == null
                || command.status() == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "ai interaction context missing");
        }
        if (!StringUtils.hasText(command.entityType())
                || !StringUtils.hasText(command.interactionType())
                || !StringUtils.hasText(command.promptVersion())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "ai interaction required fields missing");
        }

        AiInteractionRecordEntity entity = new AiInteractionRecordEntity();
        entity.setTenantId(command.tenantId());
        entity.setUserId(command.userId());
        entity.setRequestId(RequestIdPolicy.requireNormalized(command.requestId()));
        entity.setEntityType(command.entityType().trim().toUpperCase());
        entity.setEntityId(command.entityId());
        entity.setInteractionType(command.interactionType().trim().toUpperCase());
        entity.setPromptVersion(command.promptVersion().trim());
        entity.setModelId(normalizeNullable(command.modelId()));
        entity.setStatus(command.status().name());
        entity.setLatencyMs(command.latencyMs());
        entity.setOutputSummary(normalizeNullable(command.outputSummary()));
        entity.setUsagePromptTokens(command.usagePromptTokens());
        entity.setUsageCompletionTokens(command.usageCompletionTokens());
        entity.setUsageTotalTokens(command.usageTotalTokens());
        entity.setUsageCostMicros(command.usageCostMicros());
        entity.setCreatedAt(LocalDateTime.now());
        aiInteractionRecordRepository.save(entity);
    }

    private String normalizeNullable(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
