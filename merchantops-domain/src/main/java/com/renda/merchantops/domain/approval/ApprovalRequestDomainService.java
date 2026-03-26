package com.renda.merchantops.domain.approval;

import com.renda.merchantops.domain.shared.error.BizException;
import com.renda.merchantops.domain.shared.error.ErrorCode;

import java.time.LocalDateTime;
import java.util.Locale;

public class ApprovalRequestDomainService implements ApprovalRequestUseCase {

    private static final String ACTION_USER_STATUS_DISABLE = "USER_STATUS_DISABLE";
    private static final String ENTITY_USER = "USER";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 100;

    private final ApprovalRequestPort approvalRequestPort;
    private final ApprovalTargetUserPort approvalTargetUserPort;
    private final ApprovalActionPort approvalActionPort;

    public ApprovalRequestDomainService(ApprovalRequestPort approvalRequestPort,
                                        ApprovalTargetUserPort approvalTargetUserPort,
                                        ApprovalActionPort approvalActionPort) {
        this.approvalRequestPort = approvalRequestPort;
        this.approvalTargetUserPort = approvalTargetUserPort;
        this.approvalActionPort = approvalActionPort;
    }

    @Override
    public ApprovalRequestRecord createDisableRequest(Long tenantId, Long requestedBy, String requestId, Long userId) {
        requireTenantAndOperator(tenantId, requestedBy);
        String resolvedRequestId = requireRequestId(requestId);
        requireUserCanBeDisabled(tenantId, userId);
        ensureNoPendingRequest(tenantId, userId);

        return approvalRequestPort.save(new ApprovalRequestRecord(
                null,
                tenantId,
                ACTION_USER_STATUS_DISABLE,
                ENTITY_USER,
                userId,
                requestedBy,
                null,
                STATUS_PENDING,
                "{\"status\":\"DISABLED\"}",
                resolvedRequestId,
                LocalDateTime.now(),
                null,
                null
        ));
    }

    @Override
    public ApprovalRequestRecord getById(Long tenantId, Long approvalRequestId) {
        return requireApprovalRequest(tenantId, approvalRequestId);
    }

    @Override
    public ApprovalRequestPageResult page(Long tenantId, ApprovalRequestPageCriteria criteria) {
        return approvalRequestPort.page(tenantId, normalize(criteria));
    }

    @Override
    public ApprovalRequestRecord approve(Long tenantId, Long reviewerId, String requestId, Long approvalRequestId) {
        requireTenantAndOperator(tenantId, reviewerId);
        String resolvedRequestId = requireRequestId(requestId);
        ApprovalRequestRecord approvalRequest = requirePendingForUpdate(tenantId, approvalRequestId);
        ensureNotSelfReview(approvalRequest, reviewerId);
        ensureSupportedActionType(approvalRequest.actionType());
        // Re-check the target at approval time because the user may have been disabled or
        // changed by another path after the request was first created.
        requireUserCanBeDisabled(tenantId, approvalRequest.entityId());

        approvalActionPort.disableUser(tenantId, reviewerId, resolvedRequestId, approvalRequest.entityId());
        LocalDateTime now = LocalDateTime.now();
        return approvalRequestPort.save(new ApprovalRequestRecord(
                approvalRequest.id(),
                approvalRequest.tenantId(),
                approvalRequest.actionType(),
                approvalRequest.entityType(),
                approvalRequest.entityId(),
                approvalRequest.requestedBy(),
                reviewerId,
                STATUS_APPROVED,
                approvalRequest.payloadJson(),
                approvalRequest.requestId(),
                approvalRequest.createdAt(),
                now,
                now
        ));
    }

    @Override
    public ApprovalRequestRecord reject(Long tenantId, Long reviewerId, String requestId, Long approvalRequestId) {
        requireTenantAndOperator(tenantId, reviewerId);
        requireRequestId(requestId);
        ApprovalRequestRecord approvalRequest = requirePendingForUpdate(tenantId, approvalRequestId);
        ensureNotSelfReview(approvalRequest, reviewerId);

        return approvalRequestPort.save(new ApprovalRequestRecord(
                approvalRequest.id(),
                approvalRequest.tenantId(),
                approvalRequest.actionType(),
                approvalRequest.entityType(),
                approvalRequest.entityId(),
                approvalRequest.requestedBy(),
                reviewerId,
                STATUS_REJECTED,
                approvalRequest.payloadJson(),
                approvalRequest.requestId(),
                approvalRequest.createdAt(),
                LocalDateTime.now(),
                null
        ));
    }

    private void ensureNoPendingRequest(Long tenantId, Long userId) {
        if (approvalRequestPort.existsPendingDisableRequest(tenantId, userId)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "pending disable request already exists for user");
        }
    }

    private void requireTenantAndOperator(Long tenantId, Long operatorId) {
        if (tenantId == null || operatorId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "tenant or user context missing");
        }
    }

    private String requireRequestId(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "request id missing");
        }
        return requestId.trim();
    }

    private ApprovalRequestRecord requireApprovalRequest(Long tenantId, Long approvalRequestId) {
        return approvalRequestPort.findById(tenantId, approvalRequestId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "approval request not found"));
    }

    private ApprovalRequestRecord requirePendingForUpdate(Long tenantId, Long approvalRequestId) {
        ApprovalRequestRecord approvalRequest = approvalRequestPort.findByIdForUpdate(tenantId, approvalRequestId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "approval request not found"));
        if (!STATUS_PENDING.equals(approvalRequest.status())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "approval request is not pending");
        }
        return approvalRequest;
    }

    private void ensureNotSelfReview(ApprovalRequestRecord approvalRequest, Long reviewerId) {
        // Keep approval as a true second-person control instead of allowing a requester to
        // satisfy workflow requirements with a self-approval.
        if (approvalRequest.requestedBy().equals(reviewerId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "requester cannot approve or reject own request");
        }
    }

    private void ensureSupportedActionType(String actionType) {
        if (!ACTION_USER_STATUS_DISABLE.equals(normalizeKey(actionType))) {
            throw new BizException(ErrorCode.BAD_REQUEST, "unsupported approval action");
        }
    }

    private void requireUserCanBeDisabled(Long tenantId, Long userId) {
        ApprovalTargetUser user = approvalTargetUserPort.findForDisable(tenantId, userId)
                .orElseThrow(() -> new BizException(ErrorCode.BAD_REQUEST, "user must exist in current tenant"));
        if (!"ACTIVE".equals(normalizeKey(user.status()))) {
            throw new BizException(ErrorCode.BAD_REQUEST, "user is already disabled");
        }
    }

    private ApprovalRequestPageCriteria normalize(ApprovalRequestPageCriteria criteria) {
        if (criteria == null) {
            return new ApprovalRequestPageCriteria(DEFAULT_PAGE, DEFAULT_SIZE, null, null, null);
        }
        return new ApprovalRequestPageCriteria(
                normalizePage(criteria.page()),
                normalizeSize(criteria.size()),
                normalizeFilter(criteria.status()),
                normalizeFilter(criteria.actionType()),
                criteria.requestedBy()
        );
    }

    private int normalizePage(int page) {
        return page < 0 ? DEFAULT_PAGE : page;
    }

    private int normalizeSize(int size) {
        if (size <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    private String normalizeFilter(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeKey(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
