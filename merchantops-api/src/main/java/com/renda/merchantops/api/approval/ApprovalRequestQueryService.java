package com.renda.merchantops.api.approval;

import com.renda.merchantops.api.dto.approval.query.ApprovalRequestPageQuery;
import com.renda.merchantops.api.dto.approval.query.ApprovalRequestPageResponse;
import com.renda.merchantops.api.dto.approval.query.ApprovalRequestResponse;
import com.renda.merchantops.domain.approval.ApprovalRequestPageCriteria;
import com.renda.merchantops.domain.approval.ApprovalRequestUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ApprovalRequestQueryService {

    private final ApprovalRequestUseCase approvalRequestUseCase;
    private final ApprovalRequestResponseMapper approvalRequestResponseMapper;

    @Transactional(readOnly = true)
    public ApprovalRequestResponse getById(Long tenantId, Long approvalRequestId) {
        return approvalRequestResponseMapper.toResponse(approvalRequestUseCase.getById(tenantId, approvalRequestId));
    }

    @Transactional(readOnly = true)
    public ApprovalRequestPageResponse page(Long tenantId, ApprovalRequestPageQuery query) {
        var result = approvalRequestUseCase.page(tenantId, toCriteria(query));
        return new ApprovalRequestPageResponse(
                result.items().stream().map(approvalRequestResponseMapper::toListItemResponse).toList(),
                result.page(),
                result.size(),
                result.total(),
                result.totalPages()
        );
    }

    private ApprovalRequestPageCriteria toCriteria(ApprovalRequestPageQuery query) {
        if (query == null) {
            return null;
        }
        return new ApprovalRequestPageCriteria(
                query.getPage() == null ? -1 : query.getPage(),
                query.getSize() == null ? 0 : query.getSize(),
                query.getStatus(),
                query.getActionType(),
                query.getRequestedBy()
        );
    }
}
