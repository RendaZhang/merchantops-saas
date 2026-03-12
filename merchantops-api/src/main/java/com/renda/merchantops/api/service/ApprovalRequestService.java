package com.renda.merchantops.api.service;

import com.renda.merchantops.api.dto.approval.query.ApprovalRequestResponse;
import com.renda.merchantops.api.dto.user.command.UserStatusUpdateCommand;
import com.renda.merchantops.common.exception.BizException;
import com.renda.merchantops.common.exception.ErrorCode;
import com.renda.merchantops.infra.persistence.entity.ApprovalRequestEntity;
import com.renda.merchantops.infra.persistence.entity.UserEntity;
import com.renda.merchantops.infra.repository.ApprovalRequestRepository;
import com.renda.merchantops.infra.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ApprovalRequestService {

    private static final String ACTION_USER_STATUS_DISABLE = "USER_STATUS_DISABLE";
    private static final String ENTITY_USER = "USER";
    private static final String ENTITY_APPROVAL_REQUEST = "APPROVAL_REQUEST";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";

    private final ApprovalRequestRepository approvalRequestRepository;
    private final UserRepository userRepository;
    private final UserCommandService userCommandService;
    private final AuditEventService auditEventService;

    @Transactional
    public ApprovalRequestResponse createDisableRequest(Long tenantId, Long requestedBy, String requestId, Long userId) {
        requireTenantAndOperator(tenantId, requestedBy);
        String resolvedRequestId = requireRequestId(requestId);
        requireUserCanBeDisabled(tenantId, userId);
        ensureNoPendingRequest(tenantId, userId);

        ApprovalRequestEntity entity = new ApprovalRequestEntity();
        entity.setTenantId(tenantId);
        entity.setActionType(ACTION_USER_STATUS_DISABLE);
        entity.setEntityType(ENTITY_USER);
        entity.setEntityId(userId);
        entity.setRequestedBy(requestedBy);
        entity.setStatus(STATUS_PENDING);
        entity.setPayloadJson("{\"status\":\"DISABLED\"}");
        entity.setRequestId(resolvedRequestId);
        entity.setCreatedAt(LocalDateTime.now());

        ApprovalRequestEntity saved = approvalRequestRepository.save(entity);
        auditEventService.recordEvent(
                tenantId,
                ENTITY_APPROVAL_REQUEST,
                saved.getId(),
                "APPROVAL_REQUEST_CREATED",
                requestedBy,
                resolvedRequestId,
                null,
                snapshot(saved)
        );
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ApprovalRequestResponse getById(Long tenantId, Long approvalRequestId) {
        return toResponse(requireTenantApprovalRequest(tenantId, approvalRequestId));
    }

    @Transactional
    public ApprovalRequestResponse approve(Long tenantId, Long reviewerId, String requestId, Long approvalRequestId) {
        requireTenantAndOperator(tenantId, reviewerId);
        String resolvedRequestId = requireRequestId(requestId);
        ApprovalRequestEntity approvalRequest = requirePendingForUpdate(tenantId, approvalRequestId);
        ensureNotSelfReview(approvalRequest, reviewerId);
        ensureSupportedActionType(approvalRequest.getActionType());

        requireUserCanBeDisabled(tenantId, approvalRequest.getEntityId());

        LocalDateTime now = LocalDateTime.now();
        approvalRequest.setStatus(STATUS_APPROVED);
        approvalRequest.setReviewedBy(reviewerId);
        approvalRequest.setReviewedAt(now);

        userCommandService.updateStatus(
                tenantId,
                reviewerId,
                resolvedRequestId,
                approvalRequest.getEntityId(),
                new UserStatusUpdateCommand("DISABLED")
        );

        approvalRequest.setExecutedAt(LocalDateTime.now());
        ApprovalRequestEntity saved = approvalRequestRepository.save(approvalRequest);
        auditEventService.recordEvent(
                tenantId,
                ENTITY_APPROVAL_REQUEST,
                saved.getId(),
                "APPROVAL_REQUEST_APPROVED",
                reviewerId,
                resolvedRequestId,
                null,
                snapshot(saved)
        );
        auditEventService.recordEvent(
                tenantId,
                ENTITY_APPROVAL_REQUEST,
                saved.getId(),
                "APPROVAL_ACTION_EXECUTED",
                reviewerId,
                resolvedRequestId,
                null,
                Map.of("actionType", saved.getActionType(), "entityType", saved.getEntityType(), "entityId", saved.getEntityId())
        );
        return toResponse(saved);
    }

    @Transactional
    public ApprovalRequestResponse reject(Long tenantId, Long reviewerId, String requestId, Long approvalRequestId) {
        requireTenantAndOperator(tenantId, reviewerId);
        String resolvedRequestId = requireRequestId(requestId);
        ApprovalRequestEntity approvalRequest = requirePendingForUpdate(tenantId, approvalRequestId);
        ensureNotSelfReview(approvalRequest, reviewerId);

        approvalRequest.setStatus(STATUS_REJECTED);
        approvalRequest.setReviewedBy(reviewerId);
        approvalRequest.setReviewedAt(LocalDateTime.now());
        ApprovalRequestEntity saved = approvalRequestRepository.save(approvalRequest);
        auditEventService.recordEvent(
                tenantId,
                ENTITY_APPROVAL_REQUEST,
                saved.getId(),
                "APPROVAL_REQUEST_REJECTED",
                reviewerId,
                resolvedRequestId,
                null,
                snapshot(saved)
        );
        return toResponse(saved);
    }

    private void ensureNoPendingRequest(Long tenantId, Long userId) {
        boolean existsPending = approvalRequestRepository.existsByTenantIdAndActionTypeAndEntityTypeAndEntityIdAndStatus(
                tenantId,
                ACTION_USER_STATUS_DISABLE,
                ENTITY_USER,
                userId,
                STATUS_PENDING
        );
        if (existsPending) {
            throw new BizException(ErrorCode.BAD_REQUEST, "pending disable request already exists for user");
        }
    }

    private void requireTenantAndOperator(Long tenantId, Long operatorId) {
        if (tenantId == null || operatorId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "tenant or user context missing");
        }
    }

    private String requireRequestId(String requestId) {
        if (!StringUtils.hasText(requestId)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "request id missing");
        }
        return requestId;
    }

    private ApprovalRequestEntity requireTenantApprovalRequest(Long tenantId, Long approvalRequestId) {
        return approvalRequestRepository.findByIdAndTenantId(approvalRequestId, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "approval request not found"));
    }

    private ApprovalRequestEntity requirePendingForUpdate(Long tenantId, Long approvalRequestId) {
        ApprovalRequestEntity approvalRequest = approvalRequestRepository.findByIdAndTenantIdForUpdate(approvalRequestId, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "approval request not found"));
        if (!STATUS_PENDING.equals(approvalRequest.getStatus())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "approval request is not pending");
        }
        return approvalRequest;
    }

    private void ensureNotSelfReview(ApprovalRequestEntity approvalRequest, Long reviewerId) {
        if (approvalRequest.getRequestedBy().equals(reviewerId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "requester cannot approve or reject own request");
        }
    }

    private void ensureSupportedActionType(String actionType) {
        String normalized = normalizeKey(actionType);
        if (!ACTION_USER_STATUS_DISABLE.equals(normalized)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "unsupported approval action");
        }
    }

    private void requireUserCanBeDisabled(Long tenantId, Long userId) {
        UserEntity user = userRepository.findByIdAndTenantIdForUpdate(userId, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.BAD_REQUEST, "user must exist in current tenant"));
        if (!"ACTIVE".equals(normalizeKey(user.getStatus()))) {
            throw new BizException(ErrorCode.BAD_REQUEST, "user is already disabled");
        }
    }

    private String normalizeKey(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private Map<String, Object> snapshot(ApprovalRequestEntity entity) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", entity.getId());
        snapshot.put("actionType", entity.getActionType());
        snapshot.put("entityType", entity.getEntityType());
        snapshot.put("entityId", entity.getEntityId());
        snapshot.put("status", entity.getStatus());
        snapshot.put("requestedBy", entity.getRequestedBy());
        snapshot.put("reviewedBy", entity.getReviewedBy());
        snapshot.put("payloadJson", entity.getPayloadJson());
        snapshot.put("createdAt", entity.getCreatedAt());
        snapshot.put("reviewedAt", entity.getReviewedAt());
        snapshot.put("executedAt", entity.getExecutedAt());
        return snapshot;
    }

    private ApprovalRequestResponse toResponse(ApprovalRequestEntity entity) {
        return new ApprovalRequestResponse(
                entity.getId(),
                entity.getTenantId(),
                entity.getActionType(),
                entity.getEntityType(),
                entity.getEntityId(),
                entity.getRequestedBy(),
                entity.getReviewedBy(),
                entity.getStatus(),
                entity.getPayloadJson(),
                entity.getRequestId(),
                entity.getCreatedAt(),
                entity.getReviewedAt(),
                entity.getExecutedAt()
        );
    }
}
