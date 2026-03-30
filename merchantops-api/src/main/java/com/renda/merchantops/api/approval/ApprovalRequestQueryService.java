package com.renda.merchantops.api.approval;

import com.renda.merchantops.api.dto.approval.query.ApprovalRequestPageQuery;
import com.renda.merchantops.api.dto.approval.query.ApprovalRequestPageResponse;
import com.renda.merchantops.api.dto.approval.query.ApprovalRequestResponse;
import com.renda.merchantops.domain.approval.ApprovalActionTypes;
import com.renda.merchantops.domain.approval.ApprovalRequestPageCriteria;
import com.renda.merchantops.domain.approval.ApprovalRequestRecord;
import com.renda.merchantops.domain.approval.ApprovalRequestUseCase;
import com.renda.merchantops.domain.shared.error.BizException;
import com.renda.merchantops.domain.shared.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class ApprovalRequestQueryService {

    private final ApprovalRequestUseCase approvalRequestUseCase;
    private final ApprovalRequestResponseMapper approvalRequestResponseMapper;

    @Transactional(readOnly = true)
    public ApprovalRequestResponse getById(Long tenantId, Long approvalRequestId, Set<String> allowedActionTypes) {
        return approvalRequestResponseMapper.toResponse(loadVisibleRecord(tenantId, approvalRequestId, allowedActionTypes));
    }

    @Transactional(readOnly = true)
    public ApprovalRequestPageResponse page(Long tenantId, ApprovalRequestPageQuery query, Set<String> allowedActionTypes) {
        var result = approvalRequestUseCase.page(tenantId, toCriteria(query, allowedActionTypes));
        return new ApprovalRequestPageResponse(
                result.items().stream().map(approvalRequestResponseMapper::toListItemResponse).toList(),
                result.page(),
                result.size(),
                result.total(),
                result.totalPages()
        );
    }

    @Transactional(readOnly = true)
    public void requireVisibleRecord(Long tenantId, Long approvalRequestId, Set<String> allowedActionTypes) {
        requireActionAllowed(approvalRequestUseCase.getById(tenantId, approvalRequestId), allowedActionTypes);
    }

    private ApprovalRequestRecord loadVisibleRecord(Long tenantId,
                                                    Long approvalRequestId,
                                                    Set<String> allowedActionTypes) {
        ApprovalRequestRecord record = approvalRequestUseCase.getById(tenantId, approvalRequestId);
        requireActionAllowed(record, allowedActionTypes);
        return record;
    }

    private void requireActionAllowed(ApprovalRequestRecord record, Set<String> allowedActionTypes) {
        if (allowedActionTypes == null || !allowedActionTypes.contains(ApprovalActionTypes.normalize(record.actionType()))) {
            throw new BizException(ErrorCode.NOT_FOUND, "approval request not found");
        }
    }

    private ApprovalRequestPageCriteria toCriteria(ApprovalRequestPageQuery query, Set<String> allowedActionTypes) {
        if (query == null) {
            return new ApprovalRequestPageCriteria(-1, 0, null, null, null, allowedActionTypes);
        }
        return new ApprovalRequestPageCriteria(
                query.getPage() == null ? -1 : query.getPage(),
                query.getSize() == null ? 0 : query.getSize(),
                query.getStatus(),
                query.getActionType(),
                query.getRequestedBy(),
                allowedActionTypes
        );
    }
}
