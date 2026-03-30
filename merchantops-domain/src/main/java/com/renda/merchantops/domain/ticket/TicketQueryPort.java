package com.renda.merchantops.domain.ticket;

import java.util.Optional;

public interface TicketQueryPort {

    TicketPageResult pageTickets(Long tenantId, TicketPageCriteria criteria);

    boolean ticketExists(Long tenantId, Long ticketId);

    TicketAiInteractionPageResult pageTicketAiInteractions(Long tenantId,
                                                           Long ticketId,
                                                           TicketAiInteractionPageCriteria criteria);

    Optional<TicketAiInteractionItem> findTicketAiInteraction(Long tenantId, Long ticketId, Long interactionId);

    Optional<TicketDetail> findTicketDetail(Long tenantId, Long ticketId);

    Optional<TicketPromptContext> findTicketPromptContext(Long tenantId, Long ticketId);
}
