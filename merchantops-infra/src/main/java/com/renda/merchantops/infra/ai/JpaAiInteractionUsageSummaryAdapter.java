package com.renda.merchantops.infra.ai;

import com.renda.merchantops.domain.ai.AiInteractionUsageByInteractionType;
import com.renda.merchantops.domain.ai.AiInteractionUsageByStatus;
import com.renda.merchantops.domain.ai.AiInteractionUsageSummary;
import com.renda.merchantops.domain.ai.AiInteractionUsageSummaryCriteria;
import com.renda.merchantops.domain.ai.AiInteractionUsageSummaryQueryPort;
import com.renda.merchantops.infra.repository.AiInteractionRecordRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JpaAiInteractionUsageSummaryAdapter implements AiInteractionUsageSummaryQueryPort {

    private final AiInteractionRecordRepository aiInteractionRecordRepository;

    public JpaAiInteractionUsageSummaryAdapter(AiInteractionRecordRepository aiInteractionRecordRepository) {
        this.aiInteractionRecordRepository = aiInteractionRecordRepository;
    }

    @Override
    public AiInteractionUsageSummary summarize(Long tenantId, AiInteractionUsageSummaryCriteria criteria) {
        var totals = aiInteractionRecordRepository.summarizeUsageByTenant(
                tenantId,
                criteria.from(),
                criteria.to(),
                criteria.entityType(),
                criteria.interactionType(),
                criteria.status()
        );
        List<AiInteractionUsageByInteractionType> byInteractionType = aiInteractionRecordRepository
                .summarizeUsageByInteractionType(
                        tenantId,
                        criteria.from(),
                        criteria.to(),
                        criteria.entityType(),
                        criteria.interactionType(),
                        criteria.status()
                )
                .stream()
                .map(item -> new AiInteractionUsageByInteractionType(
                        item.getInteractionType(),
                        item.getInteractionCount(),
                        item.getSucceededCount(),
                        item.getFailedCount(),
                        item.getTotalTokens(),
                        item.getTotalCostMicros()
                ))
                .toList();
        List<AiInteractionUsageByStatus> byStatus = aiInteractionRecordRepository
                .summarizeUsageByStatus(
                        tenantId,
                        criteria.from(),
                        criteria.to(),
                        criteria.entityType(),
                        criteria.interactionType(),
                        criteria.status()
                )
                .stream()
                .map(item -> new AiInteractionUsageByStatus(
                        item.getStatus(),
                        item.getInteractionCount(),
                        item.getTotalTokens(),
                        item.getTotalCostMicros()
                ))
                .toList();

        return new AiInteractionUsageSummary(
                criteria.from(),
                criteria.to(),
                totals.getTotalInteractions(),
                totals.getSucceededCount(),
                totals.getFailedCount(),
                totals.getTotalPromptTokens(),
                totals.getTotalCompletionTokens(),
                totals.getTotalTokens(),
                totals.getTotalCostMicros(),
                byInteractionType,
                byStatus
        );
    }
}
