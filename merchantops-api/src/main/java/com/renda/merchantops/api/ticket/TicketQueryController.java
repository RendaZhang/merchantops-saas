package com.renda.merchantops.api.ticket;

import com.renda.merchantops.api.context.ContextAccess;
import com.renda.merchantops.api.dto.ticket.query.TicketDetailResponse;
import com.renda.merchantops.api.dto.ticket.query.TicketPageQuery;
import com.renda.merchantops.api.dto.ticket.query.TicketPageResponse;
import com.renda.merchantops.api.platform.response.ApiResponse;
import com.renda.merchantops.api.security.RequirePermission;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TicketQueryController implements TicketQueryApi {

    private final TicketQueryService ticketQueryService;

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
}
