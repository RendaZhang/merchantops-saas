package com.renda.merchantops.infra.ai;

import com.renda.merchantops.domain.ai.AiInteractionRecordPort;
import com.renda.merchantops.domain.ai.NewAiInteractionRecord;
import com.renda.merchantops.infra.persistence.entity.AiInteractionRecordEntity;
import com.renda.merchantops.infra.repository.AiInteractionRecordRepository;
import org.springframework.stereotype.Component;

@Component
public class JpaAiInteractionRecordAdapter implements AiInteractionRecordPort {

    private final AiInteractionRecordRepository aiInteractionRecordRepository;

    public JpaAiInteractionRecordAdapter(AiInteractionRecordRepository aiInteractionRecordRepository) {
        this.aiInteractionRecordRepository = aiInteractionRecordRepository;
    }

    @Override
    public void save(NewAiInteractionRecord record) {
        AiInteractionRecordEntity entity = new AiInteractionRecordEntity();
        entity.setTenantId(record.tenantId());
        entity.setUserId(record.userId());
        entity.setRequestId(record.requestId());
        entity.setEntityType(record.entityType());
        entity.setEntityId(record.entityId());
        entity.setInteractionType(record.interactionType());
        entity.setPromptVersion(record.promptVersion());
        entity.setModelId(record.modelId());
        entity.setStatus(record.status());
        entity.setLatencyMs(record.latencyMs());
        entity.setOutputSummary(record.outputSummary());
        entity.setUsagePromptTokens(record.usagePromptTokens());
        entity.setUsageCompletionTokens(record.usageCompletionTokens());
        entity.setUsageTotalTokens(record.usageTotalTokens());
        entity.setUsageCostMicros(record.usageCostMicros());
        entity.setCreatedAt(record.createdAt());
        aiInteractionRecordRepository.save(entity);
    }
}
