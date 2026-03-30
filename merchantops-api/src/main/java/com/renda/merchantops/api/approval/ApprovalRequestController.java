package com.renda.merchantops.api.approval;

import com.renda.merchantops.api.context.ContextAccess;
import com.renda.merchantops.api.context.CurrentUserContext;
import com.renda.merchantops.api.context.RequestIdAccess;
import com.renda.merchantops.api.dto.approval.query.ApprovalRequestPageQuery;
import com.renda.merchantops.api.dto.approval.query.ApprovalRequestPageResponse;
import com.renda.merchantops.api.dto.approval.query.ApprovalRequestResponse;
import com.renda.merchantops.api.platform.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.Set;

@RestController
@RequiredArgsConstructor
public class ApprovalRequestController implements ApprovalRequestApi {

    private final ApprovalRequestCommandService approvalRequestCommandService;
    private final ApprovalRequestQueryService approvalRequestQueryService;
    private final ApprovalActionPermissionRegistry approvalActionPermissionRegistry;

    @Override
    public ApiResponse<ApprovalRequestPageResponse> listApprovalRequests(ApprovalRequestPageQuery query) {
        Long tenantId = ContextAccess.requireTenantId();
        return ApiResponse.success(approvalRequestQueryService.page(tenantId, query, requireReadableActionTypes()));
    }

    @Override
    public ApiResponse<ApprovalRequestResponse> getApprovalRequest(@PathVariable("id") Long id) {
        Long tenantId = ContextAccess.requireTenantId();
        return ApiResponse.success(approvalRequestQueryService.getById(tenantId, id, requireReadableActionTypes()));
    }

    @Override
    public ApiResponse<ApprovalRequestResponse> approve(@PathVariable("id") Long id) {
        Long tenantId = ContextAccess.requireTenantId();
        Long reviewerId = ContextAccess.requireUserId();
        String requestId = RequestIdAccess.currentRequestId();
        approvalRequestQueryService.requireVisibleRecord(tenantId, id, requireReviewableActionTypes());
        return ApiResponse.success(approvalRequestCommandService.approve(tenantId, reviewerId, requestId, id));
    }

    @Override
    public ApiResponse<ApprovalRequestResponse> reject(@PathVariable("id") Long id) {
        Long tenantId = ContextAccess.requireTenantId();
        Long reviewerId = ContextAccess.requireUserId();
        String requestId = RequestIdAccess.currentRequestId();
        approvalRequestQueryService.requireVisibleRecord(tenantId, id, requireReviewableActionTypes());
        return ApiResponse.success(approvalRequestCommandService.reject(tenantId, reviewerId, requestId, id));
    }

    private Set<String> requireReadableActionTypes() {
        Set<String> actionTypes = approvalActionPermissionRegistry.readableActionTypes(currentPermissions());
        if (actionTypes.isEmpty()) {
            throw new AccessDeniedException("permission denied");
        }
        return actionTypes;
    }

    private Set<String> requireReviewableActionTypes() {
        Set<String> actionTypes = approvalActionPermissionRegistry.reviewableActionTypes(currentPermissions());
        if (actionTypes.isEmpty()) {
            throw new AccessDeniedException("permission denied");
        }
        return actionTypes;
    }

    private Collection<String> currentPermissions() {
        var currentUser = CurrentUserContext.get();
        return currentUser == null ? Set.of() : currentUser.getPermissions();
    }
}
