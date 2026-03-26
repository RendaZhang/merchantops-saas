package com.renda.merchantops.api.ticket;

import com.renda.merchantops.api.dto.ticket.command.TicketAssigneeUpdateCommand;
import com.renda.merchantops.api.dto.ticket.command.TicketCommentCreateCommand;
import com.renda.merchantops.api.dto.ticket.command.TicketCreateCommand;
import com.renda.merchantops.api.dto.ticket.command.TicketStatusUpdateCommand;
import com.renda.merchantops.api.dto.ticket.command.TicketWriteResponse;
import com.renda.merchantops.api.dto.ticket.query.TicketCommentResponse;
import com.renda.merchantops.domain.shared.error.BizException;
import com.renda.merchantops.domain.shared.error.ErrorCode;
import com.renda.merchantops.domain.ticket.AddTicketCommentCommand;
import com.renda.merchantops.domain.ticket.AssignTicketCommand;
import com.renda.merchantops.domain.ticket.TicketCommandUseCase;
import com.renda.merchantops.domain.ticket.TicketCommentResult;
import com.renda.merchantops.domain.ticket.TicketWriteResult;
import com.renda.merchantops.domain.ticket.UpdateTicketStatusCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketCommandServiceTest {

    @Mock
    private TicketCommandUseCase ticketCommandUseCase;

    @InjectMocks
    private TicketCommandService ticketCommandService;

    @Test
    void createTicketShouldNormalizeRequestIdMapCommandAndMapResult() {
        when(ticketCommandUseCase.createTicket(eq(1L), eq(101L), eq("ticket-create-req-1"), any()))
                .thenReturn(new TicketWriteResult(
                        88L,
                        1L,
                        "POS printer offline",
                        "Store printer stopped responding",
                        "OPEN",
                        null,
                        null,
                        LocalDateTime.of(2026, 3, 11, 10, 0),
                        LocalDateTime.of(2026, 3, 11, 10, 0)
                ));

        TicketWriteResponse response = ticketCommandService.createTicket(
                1L,
                101L,
                " ticket-create-req-1 ",
                new TicketCreateCommand("POS printer offline", "Store printer stopped responding")
        );

        verify(ticketCommandUseCase).createTicket(eq(1L), eq(101L), eq("ticket-create-req-1"), any(com.renda.merchantops.domain.ticket.CreateTicketCommand.class));
        assertThat(response.getId()).isEqualTo(88L);
        assertThat(response.getStatus()).isEqualTo("OPEN");
    }

    @Test
    void assignTicketShouldNormalizeRequestIdAndMapResult() {
        when(ticketCommandUseCase.assignTicket(eq(1L), eq(101L), eq("assign-req-1"), eq(11L), any(AssignTicketCommand.class)))
                .thenReturn(new TicketWriteResult(
                        11L,
                        1L,
                        "POS printer offline",
                        "desc",
                        "OPEN",
                        102L,
                        "ops",
                        LocalDateTime.of(2026, 3, 11, 10, 0),
                        LocalDateTime.of(2026, 3, 11, 10, 5)
                ));

        TicketWriteResponse response = ticketCommandService.assignTicket(
                1L,
                101L,
                " assign-req-1 ",
                11L,
                new TicketAssigneeUpdateCommand(102L)
        );

        verify(ticketCommandUseCase).assignTicket(eq(1L), eq(101L), eq("assign-req-1"), eq(11L), any(AssignTicketCommand.class));
        assertThat(response.getAssigneeId()).isEqualTo(102L);
        assertThat(response.getAssigneeUsername()).isEqualTo("ops");
    }

    @Test
    void updateStatusShouldNormalizeRequestIdAndMapResult() {
        when(ticketCommandUseCase.updateStatus(eq(1L), eq(101L), eq("status-req-1"), eq(11L), any(UpdateTicketStatusCommand.class)))
                .thenReturn(new TicketWriteResult(
                        11L,
                        1L,
                        "POS printer offline",
                        "desc",
                        "CLOSED",
                        102L,
                        "ops",
                        LocalDateTime.of(2026, 3, 11, 10, 0),
                        LocalDateTime.of(2026, 3, 11, 10, 10)
                ));

        TicketWriteResponse response = ticketCommandService.updateStatus(
                1L,
                101L,
                " status-req-1 ",
                11L,
                new TicketStatusUpdateCommand("CLOSED")
        );

        verify(ticketCommandUseCase).updateStatus(eq(1L), eq(101L), eq("status-req-1"), eq(11L), any(UpdateTicketStatusCommand.class));
        assertThat(response.getStatus()).isEqualTo("CLOSED");
    }

    @Test
    void addCommentShouldNormalizeRequestIdMapCommandAndMapResult() {
        when(ticketCommandUseCase.addComment(eq(1L), eq(102L), eq("comment-req-1"), eq(11L), any(AddTicketCommentCommand.class)))
                .thenReturn(new TicketCommentResult(
                        31L,
                        11L,
                        "Restarted the printer",
                        102L,
                        "ops",
                        LocalDateTime.of(2026, 3, 11, 10, 10)
                ));

        TicketCommentResponse response = ticketCommandService.addComment(
                1L,
                102L,
                " comment-req-1 ",
                11L,
                new TicketCommentCreateCommand("Restarted the printer")
        );

        verify(ticketCommandUseCase).addComment(eq(1L), eq(102L), eq("comment-req-1"), eq(11L), any(AddTicketCommentCommand.class));
        assertThat(response.getId()).isEqualTo(31L);
        assertThat(response.getCreatedByUsername()).isEqualTo("ops");
    }

    @Test
    void createTicketShouldRejectMissingRequestIdBeforeCallingDomain() {
        assertThatThrownBy(() -> ticketCommandService.createTicket(
                1L,
                101L,
                "   ",
                new TicketCreateCommand("POS printer offline", "desc")
        )).isInstanceOf(BizException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BAD_REQUEST);
    }
}
