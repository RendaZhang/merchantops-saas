package com.renda.merchantops.domain.ai;

import com.renda.merchantops.domain.shared.error.BizException;
import com.renda.merchantops.domain.shared.error.ErrorCode;

import java.util.Set;

public class AiInteractionUsageSummaryDomainService implements AiInteractionUsageSummaryUseCase {

    private static final Set<String> SUPPORTED_ENTITY_TYPES = Set.of("TICKET", "IMPORT_JOB");

    private final AiInteractionUsageSummaryQueryPort aiInteractionUsageSummaryQueryPort;

    public AiInteractionUsageSummaryDomainService(AiInteractionUsageSummaryQueryPort aiInteractionUsageSummaryQueryPort) {
        this.aiInteractionUsageSummaryQueryPort = aiInteractionUsageSummaryQueryPort;
    }

    @Override
    public AiInteractionUsageSummary summarize(Long tenantId, AiInteractionUsageSummaryCriteria criteria) {
        AiInteractionUsageSummaryCriteria normalized = normalize(criteria);
        return aiInteractionUsageSummaryQueryPort.summarize(tenantId, normalized);
    }

    private AiInteractionUsageSummaryCriteria normalize(AiInteractionUsageSummaryCriteria criteria) {
        if (criteria == null) {
            return new AiInteractionUsageSummaryCriteria(null, null, null, null, null);
        }

        if (criteria.from() != null && criteria.to() != null && criteria.from().isAfter(criteria.to())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "from must be before or equal to to");
        }

        String normalizedEntityType = normalizeEntityType(criteria.entityType());
        return new AiInteractionUsageSummaryCriteria(
                criteria.from(),
                criteria.to(),
                normalizedEntityType,
                normalizeFilter(criteria.interactionType()),
                normalizeFilter(criteria.status())
        );
    }

    private String normalizeEntityType(String entityType) {
        String normalized = normalizeFilter(entityType);
        if (normalized == null) {
            return null;
        }
        if (!SUPPORTED_ENTITY_TYPES.contains(normalized)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "entityType must be one of TICKET, IMPORT_JOB");
        }
        return normalized;
    }

    private String normalizeFilter(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
