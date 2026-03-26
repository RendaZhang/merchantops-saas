package com.renda.merchantops.api.ticket;

import com.renda.merchantops.api.context.ContextAccess;
import com.renda.merchantops.api.context.RequestIdAccess;
import com.renda.merchantops.api.dto.ticket.query.TicketAiInteractionPageQuery;
import com.renda.merchantops.api.dto.ticket.query.TicketAiInteractionPageResponse;
import com.renda.merchantops.api.dto.ticket.query.TicketAiReplyDraftResponse;
import com.renda.merchantops.api.dto.ticket.query.TicketAiSummaryResponse;
import com.renda.merchantops.api.dto.ticket.query.TicketAiTriageResponse;
import com.renda.merchantops.api.platform.response.ApiResponse;
import com.renda.merchantops.api.security.RequirePermission;
import com.renda.merchantops.api.ticket.ai.TicketAiReplyDraftService;
import com.renda.merchantops.api.ticket.ai.TicketAiSummaryService;
import com.renda.merchantops.api.ticket.ai.TicketAiTriageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TicketAiController implements TicketAiApi {

    private final TicketQueryService ticketQueryService;
    private final TicketAiSummaryService ticketAiSummaryService;
    private final TicketAiTriageService ticketAiTriageService;
    private final TicketAiReplyDraftService ticketAiReplyDraftService;

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
    @RequirePermission("TICKET_READ")
    public ApiResponse<TicketAiReplyDraftResponse> getAiReplyDraft(@PathVariable("id") Long id) {
        Long tenantId = ContextAccess.requireTenantId();
        Long userId = ContextAccess.requireUserId();
        String requestId = RequestIdAccess.currentRequestId();
        return ApiResponse.success(ticketAiReplyDraftService.generateReplyDraft(tenantId, userId, requestId, id));
    }

    @Override
    @RequirePermission("TICKET_READ")
    public ApiResponse<TicketAiInteractionPageResponse> listAiInteractions(@PathVariable("id") Long id,
                                                                           TicketAiInteractionPageQuery query) {
        Long tenantId = ContextAccess.requireTenantId();
        return ApiResponse.success(ticketQueryService.pageTicketAiInteractions(tenantId, id, query));
    }
}
