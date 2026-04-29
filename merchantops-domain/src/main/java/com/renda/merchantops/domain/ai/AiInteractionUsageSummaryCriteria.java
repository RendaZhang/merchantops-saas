package com.renda.merchantops.domain.ai;

import java.time.LocalDateTime;

public record AiInteractionUsageSummaryCriteria(
        LocalDateTime from,
        LocalDateTime to,
        String entityType,
        String interactionType,
        String status
) {
}
