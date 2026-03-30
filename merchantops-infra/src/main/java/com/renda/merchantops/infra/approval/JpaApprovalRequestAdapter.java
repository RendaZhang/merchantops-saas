package com.renda.merchantops.infra.approval;

import com.renda.merchantops.domain.approval.ApprovalRequestPageCriteria;
import com.renda.merchantops.domain.approval.ApprovalRequestPageResult;
import com.renda.merchantops.domain.approval.ApprovalRequestPort;
import com.renda.merchantops.domain.approval.ApprovalRequestRecord;
import com.renda.merchantops.domain.approval.ApprovalActionTypes;
import com.renda.merchantops.domain.shared.error.BizException;
import com.renda.merchantops.domain.shared.error.ErrorCode;
import com.renda.merchantops.infra.persistence.entity.ApprovalRequestEntity;
import com.renda.merchantops.infra.repository.ApprovalRequestRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class JpaApprovalRequestAdapter implements ApprovalRequestPort {

    private static final String ACTION_USER_STATUS_DISABLE = "USER_STATUS_DISABLE";
    private static final String ENTITY_USER = "USER";
    private static final String STATUS_PENDING = "PENDING";
    private static final String DUPLICATE_PENDING_DISABLE_MESSAGE = "pending disable request already exists for user";

    private final ApprovalRequestRepository approvalRequestRepository;

    public JpaApprovalRequestAdapter(ApprovalRequestRepository approvalRequestRepository) {
        this.approvalRequestRepository = approvalRequestRepository;
    }

    @Override
    public ApprovalRequestRecord save(ApprovalRequestRecord request) {
        ApprovalRequestEntity entity = new ApprovalRequestEntity();
        entity.setId(request.id());
        entity.setTenantId(request.tenantId());
        entity.setActionType(request.actionType());
        entity.setEntityType(request.entityType());
        entity.setEntityId(request.entityId());
        entity.setRequestedBy(request.requestedBy());
        entity.setReviewedBy(request.reviewedBy());
        entity.setStatus(request.status());
        entity.setPayloadJson(request.payloadJson());
        entity.setPendingRequestKey(toPendingRequestKey(request));
        entity.setRequestId(request.requestId());
        entity.setCreatedAt(request.createdAt());
        entity.setReviewedAt(request.reviewedAt());
        entity.setExecutedAt(request.executedAt());
        try {
            return toRecord(approvalRequestRepository.save(entity));
        } catch (DataIntegrityViolationException ex) {
            if (isPendingDisableRequest(request)) {
                throw new BizException(ErrorCode.BAD_REQUEST, DUPLICATE_PENDING_DISABLE_MESSAGE);
            }
            throw ex;
        }
    }

    @Override
    public Optional<ApprovalRequestRecord> findById(Long tenantId, Long approvalRequestId) {
        return approvalRequestRepository.findByIdAndTenantId(approvalRequestId, tenantId).map(this::toRecord);
    }

    @Override
    public Optional<ApprovalRequestRecord> findByIdForUpdate(Long tenantId, Long approvalRequestId) {
        return approvalRequestRepository.findByIdAndTenantIdForUpdate(approvalRequestId, tenantId).map(this::toRecord);
    }

    @Override
    public ApprovalRequestPageResult page(Long tenantId, ApprovalRequestPageCriteria criteria) {
        if (criteria.allowedActionTypes() == null || criteria.allowedActionTypes().isEmpty()) {
            return new ApprovalRequestPageResult(List.of(), criteria.page(), criteria.size(), 0, 0);
        }
        Page<ApprovalRequestEntity> resultPage = approvalRequestRepository.searchPageByTenantId(
                tenantId,
                criteria.status(),
                criteria.actionType(),
                criteria.requestedBy(),
                criteria.allowedActionTypes(),
                PageRequest.of(
                        criteria.page(),
                        criteria.size(),
                        Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))
                )
        );
        return new ApprovalRequestPageResult(
                resultPage.getContent().stream().map(this::toRecord).toList(),
                resultPage.getNumber(),
                resultPage.getSize(),
                resultPage.getTotalElements(),
                resultPage.getTotalPages()
        );
    }

    private ApprovalRequestRecord toRecord(ApprovalRequestEntity entity) {
        return new ApprovalRequestRecord(
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

    private String toPendingRequestKey(ApprovalRequestRecord request) {
        if (!isPendingDisableRequest(request)) {
            return null;
        }
        return ACTION_USER_STATUS_DISABLE + ":" + request.tenantId() + ":" + request.entityId();
    }

    private boolean isPendingDisableRequest(ApprovalRequestRecord request) {
        return ACTION_USER_STATUS_DISABLE.equals(normalizeKey(request.actionType()))
                && ENTITY_USER.equals(normalizeKey(request.entityType()))
                && STATUS_PENDING.equals(normalizeKey(request.status()))
                && request.tenantId() != null
                && request.entityId() != null;
    }

    private String normalizeKey(String value) {
        return ApprovalActionTypes.normalize(value);
    }
}
