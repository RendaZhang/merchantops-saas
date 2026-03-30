package com.renda.merchantops.domain.ticket;

import com.renda.merchantops.domain.shared.error.BizException;
import com.renda.merchantops.domain.shared.error.ErrorCode;

import java.util.Optional;

public class TicketQueryService implements TicketQueryUseCase {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 100;

    private final TicketQueryPort ticketQueryPort;

    public TicketQueryService(TicketQueryPort ticketQueryPort) {
        this.ticketQueryPort = ticketQueryPort;
    }

    @Override
    public TicketPageResult pageTickets(Long tenantId, TicketPageCriteria criteria) {
        TicketPageCriteria normalized = normalize(criteria);
        if (normalized.assigneeId() != null && normalized.unassignedOnly()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "assigneeId cannot be combined with unassignedOnly=true");
        }
        return ticketQueryPort.pageTickets(tenantId, normalized);
    }

    @Override
    public TicketAiInteractionPageResult pageTicketAiInteractions(Long tenantId,
                                                                  Long ticketId,
                                                                  TicketAiInteractionPageCriteria criteria) {
        requireTicketExists(tenantId, ticketId);
        return ticketQueryPort.pageTicketAiInteractions(tenantId, ticketId, normalize(criteria));
    }

    @Override
    public Optional<TicketAiInteractionItem> findTicketAiInteraction(Long tenantId, Long ticketId, Long interactionId) {
        requireTicketExists(tenantId, ticketId);
        return ticketQueryPort.findTicketAiInteraction(tenantId, ticketId, interactionId);
    }

    @Override
    public TicketDetail getTicketDetail(Long tenantId, Long ticketId) {
        return ticketQueryPort.findTicketDetail(tenantId, ticketId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "ticket not found"));
    }

    @Override
    public TicketPromptContext getTicketPromptContext(Long tenantId, Long ticketId) {
        return ticketQueryPort.findTicketPromptContext(tenantId, ticketId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "ticket not found"));
    }

    private void requireTicketExists(Long tenantId, Long ticketId) {
        if (!ticketQueryPort.ticketExists(tenantId, ticketId)) {
            throw new BizException(ErrorCode.NOT_FOUND, "ticket not found");
        }
    }

    private TicketPageCriteria normalize(TicketPageCriteria criteria) {
        if (criteria == null) {
            return new TicketPageCriteria(DEFAULT_PAGE, DEFAULT_SIZE, null, null, null, false);
        }
        return new TicketPageCriteria(
                normalizePage(criteria.page()),
                normalizeSize(criteria.size()),
                normalizeFilter(criteria.status()),
                criteria.assigneeId(),
                normalizeFilter(criteria.keyword()),
                criteria.unassignedOnly()
        );
    }

    private TicketAiInteractionPageCriteria normalize(TicketAiInteractionPageCriteria criteria) {
        if (criteria == null) {
            return new TicketAiInteractionPageCriteria(DEFAULT_PAGE, DEFAULT_SIZE, null, null);
        }
        return new TicketAiInteractionPageCriteria(
                normalizePage(criteria.page()),
                normalizeSize(criteria.size()),
                normalizeFilter(criteria.interactionType()),
                normalizeFilter(criteria.status())
        );
    }

    private int normalizePage(int page) {
        return page < 0 ? DEFAULT_PAGE : page;
    }

    private int normalizeSize(int size) {
        if (size <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    private String normalizeFilter(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
