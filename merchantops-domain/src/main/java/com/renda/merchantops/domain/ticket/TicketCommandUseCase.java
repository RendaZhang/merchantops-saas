package com.renda.merchantops.domain.ticket;

public interface TicketCommandUseCase {

    TicketWriteResult createTicket(Long tenantId, Long operatorId, String requestId, CreateTicketCommand command);

    TicketWriteResult assignTicket(Long tenantId,
                                   Long operatorId,
                                   String requestId,
                                   Long ticketId,
                                   AssignTicketCommand command);

    TicketWriteResult updateStatus(Long tenantId,
                                   Long operatorId,
                                   String requestId,
                                   Long ticketId,
                                   UpdateTicketStatusCommand command);

    TicketCommentResult addComment(Long tenantId,
                                   Long operatorId,
                                   String requestId,
                                   Long ticketId,
                                   AddTicketCommentCommand command);
}
