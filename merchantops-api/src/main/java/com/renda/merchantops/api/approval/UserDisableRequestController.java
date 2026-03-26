package com.renda.merchantops.api.approval;

import com.renda.merchantops.api.context.ContextAccess;
import com.renda.merchantops.api.context.RequestIdAccess;
import com.renda.merchantops.api.dto.approval.query.ApprovalRequestResponse;
import com.renda.merchantops.api.security.RequirePermission;
import com.renda.merchantops.api.platform.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserDisableRequestController implements UserDisableRequestApi {

    private final ApprovalRequestCommandService approvalRequestCommandService;

    @Override
    @RequirePermission("USER_WRITE")
    public ApiResponse<ApprovalRequestResponse> createUserDisableRequest(@PathVariable("id") Long id) {
        Long tenantId = ContextAccess.requireTenantId();
        Long requesterId = ContextAccess.requireUserId();
        String requestId = RequestIdAccess.currentRequestId();
        return ApiResponse.success(approvalRequestCommandService.createDisableRequest(tenantId, requesterId, requestId, id));
    }
}
