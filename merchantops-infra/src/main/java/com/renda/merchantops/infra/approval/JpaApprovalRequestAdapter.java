package com.renda.merchantops.infra.approval;

import com.renda.merchantops.domain.approval.ApprovalRequestPageCriteria;
import com.renda.merchantops.domain.approval.ApprovalRequestPageResult;
import com.renda.merchantops.domain.approval.ApprovalRequestPort;
import com.renda.merchantops.domain.approval.ApprovalRequestRecord;
import com.renda.merchantops.infra.persistence.entity.ApprovalRequestEntity;
import com.renda.merchantops.infra.repository.ApprovalRequestRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class JpaApprovalRequestAdapter implements ApprovalRequestPort {

    private final ApprovalRequestRepository approvalRequestRepository;

    public JpaApprovalRequestAdapter(ApprovalRequestRepository approvalRequestRepository) {
        this.approvalRequestRepository = approvalRequestRepository;
    }

    @Override
    public boolean existsPendingDisableRequest(Long tenantId, Long userId) {
        return approvalRequestRepository.existsByTenantIdAndActionTypeAndEntityTypeAndEntityIdAndStatus(
                tenantId,
                "USER_STATUS_DISABLE",
                "USER",
                userId,
                "PENDING"
        );
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
        entity.setRequestId(request.requestId());
        entity.setCreatedAt(request.createdAt());
        entity.setReviewedAt(request.reviewedAt());
        entity.setExecutedAt(request.executedAt());
        return toRecord(approvalRequestRepository.save(entity));
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
        Page<ApprovalRequestEntity> resultPage = approvalRequestRepository.searchPageByTenantId(
                tenantId,
                criteria.status(),
                criteria.actionType(),
                criteria.requestedBy(),
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
}
