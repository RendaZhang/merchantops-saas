package com.renda.merchantops.api.ticket;

import com.renda.merchantops.api.approval.ApprovalRequestCommandService;
import com.renda.merchantops.api.context.ContextAccess;
import com.renda.merchantops.api.context.RequestIdAccess;
import com.renda.merchantops.api.dto.approval.query.ApprovalRequestResponse;
import com.renda.merchantops.api.dto.ticket.command.TicketAssigneeUpdateCommand;
import com.renda.merchantops.api.dto.ticket.command.TicketAssigneeUpdateRequest;
import com.renda.merchantops.api.dto.ticket.command.TicketCommentCreateCommand;
import com.renda.merchantops.api.dto.ticket.command.TicketCommentCreateRequest;
import com.renda.merchantops.api.dto.ticket.command.TicketCommentProposalRequest;
import com.renda.merchantops.api.dto.ticket.command.TicketCreateCommand;
import com.renda.merchantops.api.dto.ticket.command.TicketCreateRequest;
import com.renda.merchantops.api.dto.ticket.command.TicketStatusUpdateCommand;
import com.renda.merchantops.api.dto.ticket.command.TicketStatusUpdateRequest;
import com.renda.merchantops.api.dto.ticket.command.TicketWriteResponse;
import com.renda.merchantops.api.dto.ticket.query.TicketCommentResponse;
import com.renda.merchantops.api.platform.response.ApiResponse;
import com.renda.merchantops.api.security.RequirePermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TicketWorkflowController implements TicketWorkflowApi {

    private final TicketCommandService ticketCommandService;
    private final ApprovalRequestCommandService approvalRequestCommandService;

    @Override
    @RequirePermission("TICKET_WRITE")
    public ApiResponse<TicketWriteResponse> createTicket(@Valid @RequestBody TicketCreateRequest request) {
        Long tenantId = ContextAccess.requireTenantId();
        Long operatorId = ContextAccess.requireUserId();
        String requestId = RequestIdAccess.currentRequestId();
        TicketCreateCommand command = new TicketCreateCommand(request.getTitle(), request.getDescription());
        return ApiResponse.success(ticketCommandService.createTicket(tenantId, operatorId, requestId, command));
    }

    @Override
    @RequirePermission("TICKET_WRITE")
    public ApiResponse<TicketWriteResponse> assignTicket(@PathVariable("id") Long id,
                                                         @Valid @RequestBody TicketAssigneeUpdateRequest request) {
        Long tenantId = ContextAccess.requireTenantId();
        Long operatorId = ContextAccess.requireUserId();
        String requestId = RequestIdAccess.currentRequestId();
        TicketAssigneeUpdateCommand command = new TicketAssigneeUpdateCommand(request.getAssigneeId());
        return ApiResponse.success(ticketCommandService.assignTicket(tenantId, operatorId, requestId, id, command));
    }

    @Override
    @RequirePermission("TICKET_WRITE")
    public ApiResponse<TicketWriteResponse> updateTicketStatus(@PathVariable("id") Long id,
                                                               @Valid @RequestBody TicketStatusUpdateRequest request) {
        Long tenantId = ContextAccess.requireTenantId();
        Long operatorId = ContextAccess.requireUserId();
        String requestId = RequestIdAccess.currentRequestId();
        TicketStatusUpdateCommand command = new TicketStatusUpdateCommand(request.getStatus());
        return ApiResponse.success(ticketCommandService.updateStatus(tenantId, operatorId, requestId, id, command));
    }

    @Override
    @RequirePermission("TICKET_WRITE")
    public ApiResponse<TicketCommentResponse> addComment(@PathVariable("id") Long id,
                                                         @Valid @RequestBody TicketCommentCreateRequest request) {
        Long tenantId = ContextAccess.requireTenantId();
        Long operatorId = ContextAccess.requireUserId();
        String requestId = RequestIdAccess.currentRequestId();
        TicketCommentCreateCommand command = new TicketCommentCreateCommand(request.getContent());
        return ApiResponse.success(ticketCommandService.addComment(tenantId, operatorId, requestId, id, command));
    }

    @Override
    @RequirePermission("TICKET_WRITE")
    public ApiResponse<ApprovalRequestResponse> createCommentProposal(@PathVariable("id") Long id,
                                                                      @Valid @RequestBody TicketCommentProposalRequest request) {
        Long tenantId = ContextAccess.requireTenantId();
        Long operatorId = ContextAccess.requireUserId();
        String requestId = RequestIdAccess.currentRequestId();
        return ApiResponse.success(approvalRequestCommandService.createTicketCommentRequest(tenantId, operatorId, requestId, id, request));
    }
}
