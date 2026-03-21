package com.renda.merchantops.api.ai;

import com.renda.merchantops.api.dto.ticket.query.TicketAiTriagePriority;

public record TicketTriageProviderResult(
        String classification,
        TicketAiTriagePriority priority,
        String reasoning,
        String modelId,
        Integer inputTokens,
        Integer outputTokens,
        Integer totalTokens,
        Long costMicros
) {
}
