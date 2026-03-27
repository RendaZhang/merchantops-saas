package com.renda.merchantops.api.importjob;

import com.renda.merchantops.api.context.ContextAccess;
import com.renda.merchantops.api.context.RequestIdAccess;
import com.renda.merchantops.api.dto.importjob.query.ImportJobAiErrorSummaryResponse;
import com.renda.merchantops.api.importjob.ai.ImportJobAiErrorSummaryService;
import com.renda.merchantops.api.platform.response.ApiResponse;
import com.renda.merchantops.api.security.RequirePermission;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ImportJobAiController implements ImportJobAiApi {

    private final ImportJobAiErrorSummaryService importJobAiErrorSummaryService;

    @Override
    @RequirePermission("USER_READ")
    public ApiResponse<ImportJobAiErrorSummaryResponse> getAiErrorSummary(@PathVariable("id") Long id) {
        Long tenantId = ContextAccess.requireTenantId();
        Long userId = ContextAccess.requireUserId();
        String requestId = RequestIdAccess.currentRequestId();
        return ApiResponse.success(importJobAiErrorSummaryService.generateErrorSummary(tenantId, userId, requestId, id));
    }
}
