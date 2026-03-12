package com.renda.merchantops.api.controller;

import com.renda.merchantops.api.context.ContextAccess;
import com.renda.merchantops.api.context.RequestIdAccess;
import com.renda.merchantops.api.contract.ApprovalRequestApi;
import com.renda.merchantops.api.dto.approval.query.ApprovalRequestResponse;
import com.renda.merchantops.api.security.RequirePermission;
import com.renda.merchantops.api.service.ApprovalRequestService;
import com.renda.merchantops.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ApprovalRequestController implements ApprovalRequestApi {

    private final ApprovalRequestService approvalRequestService;

    @Override
    @RequirePermission("USER_READ")
    public ApiResponse<ApprovalRequestResponse> getApprovalRequest(@PathVariable("id") Long id) {
        Long tenantId = ContextAccess.requireTenantId();
        return ApiResponse.success(approvalRequestService.getById(tenantId, id));
    }

    @Override
    @RequirePermission("USER_WRITE")
    public ApiResponse<ApprovalRequestResponse> approve(@PathVariable("id") Long id) {
        Long tenantId = ContextAccess.requireTenantId();
        Long reviewerId = ContextAccess.requireUserId();
        String requestId = RequestIdAccess.currentRequestId();
        return ApiResponse.success(approvalRequestService.approve(tenantId, reviewerId, requestId, id));
    }

    @Override
    @RequirePermission("USER_WRITE")
    public ApiResponse<ApprovalRequestResponse> reject(@PathVariable("id") Long id) {
        Long tenantId = ContextAccess.requireTenantId();
        Long reviewerId = ContextAccess.requireUserId();
        String requestId = RequestIdAccess.currentRequestId();
        return ApiResponse.success(approvalRequestService.reject(tenantId, reviewerId, requestId, id));
    }

}
