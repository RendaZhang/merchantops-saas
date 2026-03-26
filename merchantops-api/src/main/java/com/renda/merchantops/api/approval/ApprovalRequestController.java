package com.renda.merchantops.api.approval;

import com.renda.merchantops.api.context.ContextAccess;
import com.renda.merchantops.api.context.RequestIdAccess;
import com.renda.merchantops.api.dto.approval.query.ApprovalRequestPageQuery;
import com.renda.merchantops.api.dto.approval.query.ApprovalRequestPageResponse;
import com.renda.merchantops.api.dto.approval.query.ApprovalRequestResponse;
import com.renda.merchantops.api.platform.response.ApiResponse;
import com.renda.merchantops.api.security.RequirePermission;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ApprovalRequestController implements ApprovalRequestApi {

    private final ApprovalRequestCommandService approvalRequestCommandService;
    private final ApprovalRequestQueryService approvalRequestQueryService;

    @Override
    @RequirePermission("USER_READ")
    public ApiResponse<ApprovalRequestPageResponse> listApprovalRequests(ApprovalRequestPageQuery query) {
        Long tenantId = ContextAccess.requireTenantId();
        return ApiResponse.success(approvalRequestQueryService.page(tenantId, query));
    }

    @Override
    @RequirePermission("USER_READ")
    public ApiResponse<ApprovalRequestResponse> getApprovalRequest(@PathVariable("id") Long id) {
        Long tenantId = ContextAccess.requireTenantId();
        return ApiResponse.success(approvalRequestQueryService.getById(tenantId, id));
    }

    @Override
    @RequirePermission("USER_WRITE")
    public ApiResponse<ApprovalRequestResponse> approve(@PathVariable("id") Long id) {
        Long tenantId = ContextAccess.requireTenantId();
        Long reviewerId = ContextAccess.requireUserId();
        String requestId = RequestIdAccess.currentRequestId();
        return ApiResponse.success(approvalRequestCommandService.approve(tenantId, reviewerId, requestId, id));
    }

    @Override
    @RequirePermission("USER_WRITE")
    public ApiResponse<ApprovalRequestResponse> reject(@PathVariable("id") Long id) {
        Long tenantId = ContextAccess.requireTenantId();
        Long reviewerId = ContextAccess.requireUserId();
        String requestId = RequestIdAccess.currentRequestId();
        return ApiResponse.success(approvalRequestCommandService.reject(tenantId, reviewerId, requestId, id));
    }
}
