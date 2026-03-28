package com.renda.merchantops.domain.importjob;

public record ImportJobAiInteractionPageCriteria(
        int page,
        int size,
        String interactionType,
        String status
) {
}
