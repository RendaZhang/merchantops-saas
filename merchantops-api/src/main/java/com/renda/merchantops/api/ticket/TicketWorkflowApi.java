package com.renda.merchantops.api.ticket;

import com.renda.merchantops.api.dto.approval.query.ApprovalRequestResponse;
import com.renda.merchantops.api.dto.ticket.command.TicketAssigneeUpdateRequest;
import com.renda.merchantops.api.dto.ticket.command.TicketCommentCreateRequest;
import com.renda.merchantops.api.dto.ticket.command.TicketCommentProposalRequest;
import com.renda.merchantops.api.dto.ticket.command.TicketCreateRequest;
import com.renda.merchantops.api.dto.ticket.command.TicketStatusUpdateRequest;
import com.renda.merchantops.api.dto.ticket.command.TicketWriteResponse;
import com.renda.merchantops.api.dto.ticket.query.TicketCommentResponse;
import com.renda.merchantops.api.platform.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Ticket Workflow")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/v1/tickets")
public interface TicketWorkflowApi {

    @Operation(summary = "Create ticket in current tenant")
    @RequestBody(required = true)
    @PostMapping
    ApiResponse<TicketWriteResponse> createTicket(@Valid @org.springframework.web.bind.annotation.RequestBody TicketCreateRequest request);

    @Operation(summary = "Assign ticket to a tenant user")
    @RequestBody(required = true)
    @PatchMapping("/{id}/assignee")
    ApiResponse<TicketWriteResponse> assignTicket(@PathVariable("id") Long id,
                                                  @Valid @org.springframework.web.bind.annotation.RequestBody TicketAssigneeUpdateRequest request);

    @Operation(summary = "Transition ticket status in current tenant")
    @RequestBody(required = true)
    @PatchMapping("/{id}/status")
    ApiResponse<TicketWriteResponse> updateTicketStatus(@PathVariable("id") Long id,
                                                        @Valid @org.springframework.web.bind.annotation.RequestBody TicketStatusUpdateRequest request);

    @Operation(summary = "Add ticket comment in current tenant")
    @RequestBody(required = true)
    @PostMapping("/{id}/comments")
    ApiResponse<TicketCommentResponse> addComment(@PathVariable("id") Long id,
                                                  @Valid @org.springframework.web.bind.annotation.RequestBody TicketCommentCreateRequest request);

    @Operation(summary = "Create approval-backed ticket comment proposal from AI reply draft workflow")
    @RequestBody(required = true)
    @PostMapping("/{id}/comments/proposals/ai-reply-draft")
    ApiResponse<ApprovalRequestResponse> createCommentProposal(@PathVariable("id") Long id,
                                                               @Valid @org.springframework.web.bind.annotation.RequestBody TicketCommentProposalRequest request);
}
