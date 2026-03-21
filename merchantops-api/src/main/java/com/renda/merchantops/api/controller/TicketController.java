package com.renda.merchantops.api.controller;

import com.renda.merchantops.api.context.ContextAccess;
import com.renda.merchantops.api.context.RequestIdAccess;
import com.renda.merchantops.api.contract.TicketManagementApi;
import com.renda.merchantops.api.dto.ticket.command.TicketAssigneeUpdateCommand;
import com.renda.merchantops.api.dto.ticket.command.TicketAssigneeUpdateRequest;
import com.renda.merchantops.api.dto.ticket.command.TicketCommentCreateCommand;
import com.renda.merchantops.api.dto.ticket.command.TicketCommentCreateRequest;
import com.renda.merchantops.api.dto.ticket.command.TicketCreateCommand;
import com.renda.merchantops.api.dto.ticket.command.TicketCreateRequest;
import com.renda.merchantops.api.dto.ticket.command.TicketStatusUpdateCommand;
import com.renda.merchantops.api.dto.ticket.command.TicketStatusUpdateRequest;
import com.renda.merchantops.api.dto.ticket.command.TicketWriteResponse;
import com.renda.merchantops.api.dto.ticket.query.TicketAiTriageResponse;
import com.renda.merchantops.api.dto.ticket.query.TicketAiSummaryResponse;
import com.renda.merchantops.api.dto.ticket.query.TicketCommentResponse;
import com.renda.merchantops.api.dto.ticket.query.TicketDetailResponse;
import com.renda.merchantops.api.dto.ticket.query.TicketPageQuery;
import com.renda.merchantops.api.dto.ticket.query.TicketPageResponse;
import com.renda.merchantops.api.security.RequirePermission;
import com.renda.merchantops.api.service.TicketAiTriageService;
import com.renda.merchantops.api.service.TicketAiSummaryService;
import com.renda.merchantops.api.service.TicketCommandService;
import com.renda.merchantops.api.service.TicketQueryService;
import com.renda.merchantops.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TicketController implements TicketManagementApi {

    private final TicketQueryService ticketQueryService;
    private final TicketCommandService ticketCommandService;
    private final TicketAiSummaryService ticketAiSummaryService;
    private final TicketAiTriageService ticketAiTriageService;

    @Override
    @RequirePermission("TICKET_READ")
    public ApiResponse<TicketPageResponse> listTickets(TicketPageQuery query) {
        Long tenantId = ContextAccess.requireTenantId();
        return ApiResponse.success(ticketQueryService.pageTickets(tenantId, query));
    }

    @Override
    @RequirePermission("TICKET_READ")
    public ApiResponse<TicketDetailResponse> getTicketDetail(@PathVariable("id") Long id) {
        Long tenantId = ContextAccess.requireTenantId();
        return ApiResponse.success(ticketQueryService.getTicketDetail(tenantId, id));
    }

    @Override
    @RequirePermission("TICKET_READ")
    public ApiResponse<TicketAiSummaryResponse> getAiSummary(@PathVariable("id") Long id) {
        Long tenantId = ContextAccess.requireTenantId();
        Long userId = ContextAccess.requireUserId();
        String requestId = RequestIdAccess.currentRequestId();
        return ApiResponse.success(ticketAiSummaryService.generateSummary(tenantId, userId, requestId, id));
    }

    @Override
    @RequirePermission("TICKET_READ")
    public ApiResponse<TicketAiTriageResponse> getAiTriage(@PathVariable("id") Long id) {
        Long tenantId = ContextAccess.requireTenantId();
        Long userId = ContextAccess.requireUserId();
        String requestId = RequestIdAccess.currentRequestId();
        return ApiResponse.success(ticketAiTriageService.generateTriage(tenantId, userId, requestId, id));
    }

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
}
