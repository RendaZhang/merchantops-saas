package com.renda.merchantops.api.ticket;

import com.renda.merchantops.api.context.RequestIdPolicy;
import com.renda.merchantops.api.dto.ticket.command.TicketAssigneeUpdateCommand;
import com.renda.merchantops.api.dto.ticket.command.TicketCommentCreateCommand;
import com.renda.merchantops.api.dto.ticket.command.TicketCreateCommand;
import com.renda.merchantops.api.dto.ticket.command.TicketStatusUpdateCommand;
import com.renda.merchantops.api.dto.ticket.command.TicketWriteResponse;
import com.renda.merchantops.api.dto.ticket.query.TicketCommentResponse;
import com.renda.merchantops.domain.ticket.AddTicketCommentCommand;
import com.renda.merchantops.domain.ticket.AssignTicketCommand;
import com.renda.merchantops.domain.ticket.TicketCommandUseCase;
import com.renda.merchantops.domain.ticket.TicketCommentResult;
import com.renda.merchantops.domain.ticket.TicketWriteResult;
import com.renda.merchantops.domain.ticket.UpdateTicketStatusCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TicketCommandService {

    private final TicketCommandUseCase ticketCommandUseCase;

    @Transactional
    public TicketWriteResponse createTicket(Long tenantId, Long operatorId, String requestId, TicketCreateCommand command) {
        return toWriteResponse(ticketCommandUseCase.createTicket(
                tenantId,
                operatorId,
                RequestIdPolicy.requireNormalized(requestId),
                new com.renda.merchantops.domain.ticket.CreateTicketCommand(
                        command == null ? null : command.getTitle(),
                        command == null ? null : command.getDescription()
                )
        ));
    }

    @Transactional
    public TicketWriteResponse assignTicket(Long tenantId,
                                            Long operatorId,
                                            String requestId,
                                            Long ticketId,
                                            TicketAssigneeUpdateCommand command) {
        return toWriteResponse(ticketCommandUseCase.assignTicket(
                tenantId,
                operatorId,
                RequestIdPolicy.requireNormalized(requestId),
                ticketId,
                new AssignTicketCommand(command == null ? null : command.getAssigneeId())
        ));
    }

    @Transactional
    public TicketWriteResponse updateStatus(Long tenantId,
                                            Long operatorId,
                                            String requestId,
                                            Long ticketId,
                                            TicketStatusUpdateCommand command) {
        return toWriteResponse(ticketCommandUseCase.updateStatus(
                tenantId,
                operatorId,
                RequestIdPolicy.requireNormalized(requestId),
                ticketId,
                new UpdateTicketStatusCommand(command == null ? null : command.getStatus())
        ));
    }

    @Transactional
    public TicketCommentResponse addComment(Long tenantId,
                                            Long operatorId,
                                            String requestId,
                                            Long ticketId,
                                            TicketCommentCreateCommand command) {
        return toCommentResponse(ticketCommandUseCase.addComment(
                tenantId,
                operatorId,
                RequestIdPolicy.requireNormalized(requestId),
                ticketId,
                new AddTicketCommentCommand(command == null ? null : command.getContent())
        ));
    }

    private TicketWriteResponse toWriteResponse(TicketWriteResult ticket) {
        return new TicketWriteResponse(
                ticket.id(),
                ticket.tenantId(),
                ticket.title(),
                ticket.description(),
                ticket.status(),
                ticket.assigneeId(),
                ticket.assigneeUsername(),
                ticket.createdAt(),
                ticket.updatedAt()
        );
    }

    private TicketCommentResponse toCommentResponse(TicketCommentResult comment) {
        return new TicketCommentResponse(
                comment.id(),
                comment.ticketId(),
                comment.content(),
                comment.createdBy(),
                comment.createdByUsername(),
                comment.createdAt()
        );
    }
}
