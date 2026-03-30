package com.renda.merchantops.domain.ticket;

import java.util.Optional;

public interface TicketQueryUseCase {

    TicketPageResult pageTickets(Long tenantId, TicketPageCriteria criteria);

    TicketAiInteractionPageResult pageTicketAiInteractions(Long tenantId,
                                                           Long ticketId,
                                                           TicketAiInteractionPageCriteria criteria);

    Optional<TicketAiInteractionItem> findTicketAiInteraction(Long tenantId, Long ticketId, Long interactionId);

    TicketDetail getTicketDetail(Long tenantId, Long ticketId);

    TicketPromptContext getTicketPromptContext(Long tenantId, Long ticketId);
}
