package com.renda.merchantops.api.ticket;

import com.renda.merchantops.api.dto.ticket.query.TicketAiInteractionListItemResponse;
import com.renda.merchantops.api.dto.ticket.query.TicketAiInteractionPageQuery;
import com.renda.merchantops.api.dto.ticket.query.TicketAiInteractionPageResponse;
import com.renda.merchantops.api.dto.ticket.query.TicketCommentResponse;
import com.renda.merchantops.api.dto.ticket.query.TicketDetailResponse;
import com.renda.merchantops.api.dto.ticket.query.TicketListItemResponse;
import com.renda.merchantops.api.dto.ticket.query.TicketOperationLogResponse;
import com.renda.merchantops.api.dto.ticket.query.TicketPageQuery;
import com.renda.merchantops.api.dto.ticket.query.TicketPageResponse;
import com.renda.merchantops.domain.ticket.TicketAiInteractionItem;
import com.renda.merchantops.domain.ticket.TicketAiInteractionPageCriteria;
import com.renda.merchantops.domain.ticket.TicketCommentView;
import com.renda.merchantops.domain.ticket.TicketDetail;
import com.renda.merchantops.domain.ticket.TicketListItem;
import com.renda.merchantops.domain.ticket.TicketOperationLogView;
import com.renda.merchantops.domain.ticket.TicketPageCriteria;
import com.renda.merchantops.domain.ticket.TicketQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TicketQueryService {

    private final TicketQueryUseCase ticketQueryUseCase;

    public TicketPageResponse pageTickets(Long tenantId, TicketPageQuery query) {
        var result = ticketQueryUseCase.pageTickets(tenantId, toCriteria(query));
        return new TicketPageResponse(
                result.items().stream().map(this::toListItemResponse).toList(),
                result.page(),
                result.size(),
                result.total(),
                result.totalPages()
        );
    }

    public TicketAiInteractionPageResponse pageTicketAiInteractions(Long tenantId,
                                                                    Long ticketId,
                                                                    TicketAiInteractionPageQuery query) {
        var result = ticketQueryUseCase.pageTicketAiInteractions(tenantId, ticketId, toCriteria(query));
        return new TicketAiInteractionPageResponse(
                result.items().stream().map(this::toAiInteractionListItemResponse).toList(),
                result.page(),
                result.size(),
                result.total(),
                result.totalPages()
        );
    }

    public TicketDetailResponse getTicketDetail(Long tenantId, Long ticketId) {
        TicketDetail ticket = ticketQueryUseCase.getTicketDetail(tenantId, ticketId);
        return new TicketDetailResponse(
                ticket.id(),
                ticket.tenantId(),
                ticket.title(),
                ticket.description(),
                ticket.status(),
                ticket.assigneeId(),
                ticket.assigneeUsername(),
                ticket.createdBy(),
                ticket.createdByUsername(),
                ticket.createdAt(),
                ticket.updatedAt(),
                ticket.comments().stream().map(this::toCommentResponse).toList(),
                ticket.operationLogs().stream().map(this::toOperationLogResponse).toList()
        );
    }

    private TicketPageCriteria toCriteria(TicketPageQuery query) {
        if (query == null) {
            return null;
        }
        return new TicketPageCriteria(
                query.getPage() == null ? -1 : query.getPage(),
                query.getSize() == null ? 0 : query.getSize(),
                query.getStatus(),
                query.getAssigneeId(),
                query.getKeyword(),
                Boolean.TRUE.equals(query.getUnassignedOnly())
        );
    }

    private TicketAiInteractionPageCriteria toCriteria(TicketAiInteractionPageQuery query) {
        if (query == null) {
            return null;
        }
        return new TicketAiInteractionPageCriteria(
                query.getPage() == null ? -1 : query.getPage(),
                query.getSize() == null ? 0 : query.getSize(),
                query.getInteractionType(),
                query.getStatus()
        );
    }

    private TicketListItemResponse toListItemResponse(TicketListItem ticket) {
        return new TicketListItemResponse(
                ticket.id(),
                ticket.title(),
                ticket.status(),
                ticket.assigneeId(),
                ticket.assigneeUsername(),
                ticket.createdAt(),
                ticket.updatedAt()
        );
    }

    private TicketAiInteractionListItemResponse toAiInteractionListItemResponse(TicketAiInteractionItem record) {
        return new TicketAiInteractionListItemResponse(
                record.id(),
                record.interactionType(),
                record.status(),
                record.outputSummary(),
                record.promptVersion(),
                record.modelId(),
                record.latencyMs(),
                record.requestId(),
                record.usagePromptTokens(),
                record.usageCompletionTokens(),
                record.usageTotalTokens(),
                record.usageCostMicros(),
                record.createdAt()
        );
    }

    private TicketCommentResponse toCommentResponse(TicketCommentView comment) {
        return new TicketCommentResponse(
                comment.id(),
                comment.ticketId(),
                comment.content(),
                comment.createdBy(),
                comment.createdByUsername(),
                comment.createdAt()
        );
    }

    private TicketOperationLogResponse toOperationLogResponse(TicketOperationLogView log) {
        return new TicketOperationLogResponse(
                log.id(),
                log.operationType(),
                log.detail(),
                log.operatorId(),
                log.operatorUsername(),
                log.createdAt()
        );
    }
}
