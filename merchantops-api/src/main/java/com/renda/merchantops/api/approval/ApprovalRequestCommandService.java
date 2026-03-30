package com.renda.merchantops.api.approval;

import com.renda.merchantops.api.audit.AuditEventService;
import com.renda.merchantops.api.context.RequestIdPolicy;
import com.renda.merchantops.api.dto.approval.query.ApprovalRequestResponse;
import com.renda.merchantops.api.dto.importjob.command.ImportJobSelectiveReplayProposalRequest;
import com.renda.merchantops.api.dto.ticket.command.TicketCommentProposalRequest;
import com.renda.merchantops.domain.approval.ImportSelectiveReplayApprovalCommand;
import com.renda.merchantops.domain.approval.ApprovalRequestRecord;
import com.renda.merchantops.domain.approval.ApprovalRequestUseCase;
import com.renda.merchantops.domain.approval.TicketCommentApprovalCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ApprovalRequestCommandService {

    private static final String ENTITY_APPROVAL_REQUEST = "APPROVAL_REQUEST";

    private final ApprovalRequestUseCase approvalRequestUseCase;
    private final AuditEventService auditEventService;
    private final ApprovalRequestResponseMapper approvalRequestResponseMapper;

    @Transactional
    public ApprovalRequestResponse createDisableRequest(Long tenantId, Long requestedBy, String requestId, Long userId) {
        String resolvedRequestId = RequestIdPolicy.requireNormalized(requestId);
        ApprovalRequestRecord saved = approvalRequestUseCase.createDisableRequest(tenantId, requestedBy, resolvedRequestId, userId);
        auditEventService.recordEvent(
                tenantId,
                ENTITY_APPROVAL_REQUEST,
                saved.id(),
                "APPROVAL_REQUEST_CREATED",
                requestedBy,
                resolvedRequestId,
                null,
                approvalRequestResponseMapper.snapshot(saved)
        );
        return approvalRequestResponseMapper.toResponse(saved);
    }

    @Transactional
    public ApprovalRequestResponse createImportSelectiveReplayRequest(Long tenantId,
                                                                      Long requestedBy,
                                                                      String requestId,
                                                                      Long sourceJobId,
                                                                      ImportJobSelectiveReplayProposalRequest request) {
        String resolvedRequestId = RequestIdPolicy.requireNormalized(requestId);
        ApprovalRequestRecord saved = approvalRequestUseCase.createImportSelectiveReplayRequest(
                tenantId,
                requestedBy,
                resolvedRequestId,
                new ImportSelectiveReplayApprovalCommand(
                        sourceJobId,
                        request == null ? null : request.getErrorCodes(),
                        request == null ? null : request.getSourceInteractionId(),
                        request == null ? null : request.getProposalReason()
                )
        );
        auditEventService.recordEvent(
                tenantId,
                ENTITY_APPROVAL_REQUEST,
                saved.id(),
                "APPROVAL_REQUEST_CREATED",
                requestedBy,
                resolvedRequestId,
                null,
                approvalRequestResponseMapper.snapshot(saved)
        );
        return approvalRequestResponseMapper.toResponse(saved);
    }

    @Transactional
    public ApprovalRequestResponse createTicketCommentRequest(Long tenantId,
                                                              Long requestedBy,
                                                              String requestId,
                                                              Long ticketId,
                                                              TicketCommentProposalRequest request) {
        String resolvedRequestId = RequestIdPolicy.requireNormalized(requestId);
        ApprovalRequestRecord saved = approvalRequestUseCase.createTicketCommentRequest(
                tenantId,
                requestedBy,
                resolvedRequestId,
                new TicketCommentApprovalCommand(
                        ticketId,
                        request == null ? null : request.getCommentContent(),
                        request == null ? null : request.getSourceInteractionId()
                )
        );
        auditEventService.recordEvent(
                tenantId,
                ENTITY_APPROVAL_REQUEST,
                saved.id(),
                "APPROVAL_REQUEST_CREATED",
                requestedBy,
                resolvedRequestId,
                null,
                approvalRequestResponseMapper.snapshot(saved)
        );
        return approvalRequestResponseMapper.toResponse(saved);
    }

    @Transactional
    public ApprovalRequestResponse approve(Long tenantId, Long reviewerId, String requestId, Long approvalRequestId) {
        String resolvedRequestId = RequestIdPolicy.requireNormalized(requestId);
        ApprovalRequestRecord saved = approvalRequestUseCase.approve(tenantId, reviewerId, resolvedRequestId, approvalRequestId);
        auditEventService.recordEvent(
                tenantId,
                ENTITY_APPROVAL_REQUEST,
                saved.id(),
                "APPROVAL_REQUEST_APPROVED",
                reviewerId,
                resolvedRequestId,
                null,
                approvalRequestResponseMapper.snapshot(saved)
        );
        auditEventService.recordEvent(
                tenantId,
                ENTITY_APPROVAL_REQUEST,
                saved.id(),
                "APPROVAL_ACTION_EXECUTED",
                reviewerId,
                resolvedRequestId,
                null,
                Map.of("actionType", saved.actionType(), "entityType", saved.entityType(), "entityId", saved.entityId())
        );
        return approvalRequestResponseMapper.toResponse(saved);
    }

    @Transactional
    public ApprovalRequestResponse reject(Long tenantId, Long reviewerId, String requestId, Long approvalRequestId) {
        String resolvedRequestId = RequestIdPolicy.requireNormalized(requestId);
        ApprovalRequestRecord saved = approvalRequestUseCase.reject(tenantId, reviewerId, resolvedRequestId, approvalRequestId);
        auditEventService.recordEvent(
                tenantId,
                ENTITY_APPROVAL_REQUEST,
                saved.id(),
                "APPROVAL_REQUEST_REJECTED",
                reviewerId,
                resolvedRequestId,
                null,
                approvalRequestResponseMapper.snapshot(saved)
        );
        return approvalRequestResponseMapper.toResponse(saved);
    }
}
