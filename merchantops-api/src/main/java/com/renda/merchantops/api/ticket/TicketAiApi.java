package com.renda.merchantops.api.ticket;

import com.renda.merchantops.api.dto.ticket.query.TicketAiInteractionPageQuery;
import com.renda.merchantops.api.dto.ticket.query.TicketAiInteractionPageResponse;
import com.renda.merchantops.api.dto.ticket.query.TicketAiReplyDraftResponse;
import com.renda.merchantops.api.dto.ticket.query.TicketAiSummaryResponse;
import com.renda.merchantops.api.dto.ticket.query.TicketAiTriageResponse;
import com.renda.merchantops.api.platform.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Ticket Workflow")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/v1/tickets")
public interface TicketAiApi {

    @Operation(summary = "Generate AI summary for one current-tenant ticket")
    @PostMapping("/{id}/ai-summary")
    ApiResponse<TicketAiSummaryResponse> getAiSummary(@PathVariable("id") Long id);

    @Operation(summary = "Generate AI triage suggestion for one current-tenant ticket")
    @PostMapping("/{id}/ai-triage")
    ApiResponse<TicketAiTriageResponse> getAiTriage(@PathVariable("id") Long id);

    @Operation(summary = "Generate AI internal reply draft for one current-tenant ticket")
    @PostMapping("/{id}/ai-reply-draft")
    ApiResponse<TicketAiReplyDraftResponse> getAiReplyDraft(@PathVariable("id") Long id);

    @Operation(summary = "Page AI interaction history for one current-tenant ticket")
    @GetMapping("/{id}/ai-interactions")
    ApiResponse<TicketAiInteractionPageResponse> listAiInteractions(@PathVariable("id") Long id,
                                                                    @ParameterObject TicketAiInteractionPageQuery query);
}
