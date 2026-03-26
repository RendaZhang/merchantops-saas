package com.renda.merchantops.domain.ticket;

import java.util.Optional;

public interface TicketCommandPort {

    Optional<ManagedTicket> findManagedTicket(Long tenantId, Long ticketId);

    Optional<TicketUserAccount> findTenantUser(Long tenantId, Long userId);

    ManagedTicket createTicket(NewTicketDraft draft);

    ManagedTicket saveTicket(TicketUpdateDraft draft);

    StoredTicketComment createComment(NewTicketCommentDraft draft);

    void appendOperationLog(NewTicketOperationLogDraft draft);
}
