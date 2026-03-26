package com.renda.merchantops.api.ticket;

import com.renda.merchantops.api.dto.ticket.query.TicketDetailResponse;
import com.renda.merchantops.api.dto.ticket.query.TicketPageQuery;
import com.renda.merchantops.api.dto.ticket.query.TicketPageResponse;
import com.renda.merchantops.api.platform.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Ticket Workflow")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/v1/tickets")
public interface TicketQueryApi {

    @Operation(summary = "Page tickets in current tenant")
    @GetMapping
    ApiResponse<TicketPageResponse> listTickets(@ParameterObject TicketPageQuery query);

    @Operation(summary = "Get ticket detail in current tenant")
    @GetMapping("/{id}")
    ApiResponse<TicketDetailResponse> getTicketDetail(@PathVariable("id") Long id);
}
