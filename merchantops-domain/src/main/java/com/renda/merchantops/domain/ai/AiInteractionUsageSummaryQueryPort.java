package com.renda.merchantops.domain.ai;

public interface AiInteractionUsageSummaryQueryPort {

    AiInteractionUsageSummary summarize(Long tenantId, AiInteractionUsageSummaryCriteria criteria);
}
