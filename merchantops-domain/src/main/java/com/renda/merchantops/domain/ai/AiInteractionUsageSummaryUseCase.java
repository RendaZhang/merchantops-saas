package com.renda.merchantops.domain.ai;

public interface AiInteractionUsageSummaryUseCase {

    AiInteractionUsageSummary summarize(Long tenantId, AiInteractionUsageSummaryCriteria criteria);
}
