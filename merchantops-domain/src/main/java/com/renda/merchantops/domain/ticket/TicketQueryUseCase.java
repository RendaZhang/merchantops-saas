package com.renda.merchantops.domain.ticket;

public interface TicketQueryUseCase {

    TicketPageResult pageTickets(Long tenantId, TicketPageCriteria criteria);

    TicketAiInteractionPageResult pageTicketAiInteractions(Long tenantId,
                                                           Long ticketId,
                                                           TicketAiInteractionPageCriteria criteria);

    TicketDetail getTicketDetail(Long tenantId, Long ticketId);

    TicketPromptContext getTicketPromptContext(Long tenantId, Long ticketId);
}
