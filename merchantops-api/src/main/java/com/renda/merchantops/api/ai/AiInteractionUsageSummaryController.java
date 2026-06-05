package com.renda.merchantops.api.ai;

import com.renda.merchantops.api.context.ContextAccess;
import com.renda.merchantops.api.dto.ai.query.AiInteractionUsageSummaryQuery;
import com.renda.merchantops.api.dto.ai.query.AiInteractionUsageSummaryResponse;
import com.renda.merchantops.api.platform.response.ApiResponse;
import com.renda.merchantops.api.security.RequirePermission;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AiInteractionUsageSummaryController implements AiInteractionUsageSummaryApi {

    private final AiInteractionUsageSummaryQueryService aiInteractionUsageSummaryQueryService;

    @Override
    @RequirePermission("USER_READ")
    public ApiResponse<AiInteractionUsageSummaryResponse> getUsageSummary(AiInteractionUsageSummaryQuery query) {
        Long tenantId = ContextAccess.requireTenantId();
        return ApiResponse.success(aiInteractionUsageSummaryQueryService.getUsageSummary(tenantId, query));
    }
}
