package com.renda.merchantops.api.ai;

import com.renda.merchantops.api.dto.ai.query.AiInteractionUsageSummaryQuery;
import com.renda.merchantops.api.dto.ai.query.AiInteractionUsageSummaryResponse;
import com.renda.merchantops.domain.ai.AiInteractionUsageSummaryCriteria;
import com.renda.merchantops.domain.ai.AiInteractionUsageSummaryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiInteractionUsageSummaryQueryService {

    private final AiInteractionUsageSummaryUseCase aiInteractionUsageSummaryUseCase;

    public AiInteractionUsageSummaryResponse getUsageSummary(Long tenantId, AiInteractionUsageSummaryQuery query) {
        var result = aiInteractionUsageSummaryUseCase.summarize(tenantId, toCriteria(query));
        return new AiInteractionUsageSummaryResponse(
                result.from(),
                result.to(),
                result.totalInteractions(),
                result.succeededCount(),
                result.failedCount(),
                result.totalPromptTokens(),
                result.totalCompletionTokens(),
                result.totalTokens(),
                result.totalCostMicros(),
                result.byInteractionType().stream()
                        .map(item -> new AiInteractionUsageSummaryResponse.ByInteractionType(
                                item.interactionType(),
                                item.count(),
                                item.succeededCount(),
                                item.failedCount(),
                                item.totalTokens(),
                                item.totalCostMicros()
                        ))
                        .toList(),
                result.byStatus().stream()
                        .map(item -> new AiInteractionUsageSummaryResponse.ByStatus(
                                item.status(),
                                item.count(),
                                item.totalTokens(),
                                item.totalCostMicros()
                        ))
                        .toList(),
                result.byPromptVersion().stream()
                        .map(item -> new AiInteractionUsageSummaryResponse.ByPromptVersion(
                                item.promptVersion(),
                                item.count(),
                                item.succeededCount(),
                                item.failedCount(),
                                item.totalTokens(),
                                item.totalCostMicros()
                        ))
                        .toList()
        );
    }

    private AiInteractionUsageSummaryCriteria toCriteria(AiInteractionUsageSummaryQuery query) {
        if (query == null) {
            return null;
        }
        return new AiInteractionUsageSummaryCriteria(
                query.getFrom(),
                query.getTo(),
                query.getEntityType(),
                query.getInteractionType(),
                query.getStatus()
        );
    }
}
