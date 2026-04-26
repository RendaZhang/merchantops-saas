package com.renda.merchantops.api.featureflag;

import com.renda.merchantops.api.context.ContextAccess;
import com.renda.merchantops.api.context.RequestIdAccess;
import com.renda.merchantops.api.dto.featureflag.command.FeatureFlagUpdateRequest;
import com.renda.merchantops.api.dto.featureflag.query.FeatureFlagItemResponse;
import com.renda.merchantops.api.dto.featureflag.query.FeatureFlagListResponse;
import com.renda.merchantops.api.platform.response.ApiResponse;
import com.renda.merchantops.api.security.RequirePermission;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class FeatureFlagController implements FeatureFlagApi {

    private final FeatureFlagQueryService featureFlagQueryService;
    private final FeatureFlagCommandService featureFlagCommandService;

    @Override
    @RequirePermission("FEATURE_FLAG_MANAGE")
    public ApiResponse<FeatureFlagListResponse> listFeatureFlags() {
        Long tenantId = ContextAccess.requireTenantId();
        return ApiResponse.success(featureFlagQueryService.listFlags(tenantId));
    }

    @Override
    @RequirePermission("FEATURE_FLAG_MANAGE")
    public ApiResponse<FeatureFlagItemResponse> updateFeatureFlag(@PathVariable("key") String key,
                                                                  FeatureFlagUpdateRequest request) {
        Long tenantId = ContextAccess.requireTenantId();
        Long operatorId = ContextAccess.requireUserId();
        String requestId = RequestIdAccess.currentRequestId();
        return ApiResponse.success(featureFlagCommandService.updateFlag(tenantId, operatorId, requestId, key, request));
    }
}
