package com.renda.merchantops.api.featureflag;

import com.renda.merchantops.api.audit.AuditEventService;
import com.renda.merchantops.api.context.RequestIdPolicy;
import com.renda.merchantops.api.dto.featureflag.command.FeatureFlagUpdateRequest;
import com.renda.merchantops.api.dto.featureflag.query.FeatureFlagItemResponse;
import com.renda.merchantops.domain.featureflag.FeatureFlagCommandUseCase;
import com.renda.merchantops.domain.featureflag.FeatureFlagItem;
import com.renda.merchantops.domain.featureflag.FeatureFlagQueryUseCase;
import com.renda.merchantops.domain.featureflag.FeatureFlagWriteResult;
import com.renda.merchantops.domain.featureflag.UpdateFeatureFlagCommand;
import com.renda.merchantops.domain.shared.error.BizException;
import com.renda.merchantops.domain.shared.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FeatureFlagCommandService {

    private static final String ENTITY_FEATURE_FLAG = "FEATURE_FLAG";
    private static final String ACTION_FEATURE_FLAG_UPDATED = "FEATURE_FLAG_UPDATED";

    private final FeatureFlagQueryUseCase featureFlagQueryUseCase;
    private final FeatureFlagCommandUseCase featureFlagCommandUseCase;
    private final FeatureFlagQueryService featureFlagQueryService;
    private final AuditEventService auditEventService;

    @Transactional
    public FeatureFlagItemResponse updateFlag(Long tenantId,
                                              Long operatorId,
                                              String requestId,
                                              String key,
                                              FeatureFlagUpdateRequest request) {
        String resolvedRequestId = RequestIdPolicy.requireNormalized(requestId);
        FeatureFlagItem current = featureFlagQueryUseCase.findByKey(tenantId, key)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "feature flag not found"));
        boolean requestedEnabled = requireEnabled(request == null ? null : request.getEnabled());

        if (current.enabled() == requestedEnabled) {
            return featureFlagQueryService.toResponse(current);
        }

        FeatureFlagWriteResult saved = featureFlagCommandUseCase.updateFlag(
                tenantId,
                operatorId,
                key,
                new UpdateFeatureFlagCommand(requestedEnabled)
        );

        auditEventService.recordEvent(
                tenantId,
                ENTITY_FEATURE_FLAG,
                saved.id(),
                ACTION_FEATURE_FLAG_UPDATED,
                operatorId,
                resolvedRequestId,
                snapshot(current.id(), current.tenantId(), current.key(), current.enabled(), current.updatedBy(), current.createdAt(), current.updatedAt()),
                snapshot(saved.id(), saved.tenantId(), saved.key(), saved.enabled(), saved.updatedBy(), saved.createdAt(), saved.updatedAt())
        );

        return new FeatureFlagItemResponse(saved.id(), saved.key(), saved.enabled(), saved.updatedAt());
    }

    private boolean requireEnabled(Boolean enabled) {
        if (enabled == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "enabled must not be null");
        }
        return enabled;
    }

    private Map<String, Object> snapshot(Long id,
                                         Long tenantId,
                                         String key,
                                         boolean enabled,
                                         Long updatedBy,
                                         java.time.LocalDateTime createdAt,
                                         java.time.LocalDateTime updatedAt) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", id);
        snapshot.put("tenantId", tenantId);
        snapshot.put("key", key);
        snapshot.put("enabled", enabled);
        snapshot.put("updatedBy", updatedBy);
        snapshot.put("createdAt", createdAt);
        snapshot.put("updatedAt", updatedAt);
        return snapshot;
    }
}
