package com.renda.merchantops.domain.ticket;

import com.renda.merchantops.domain.shared.error.BizException;
import com.renda.merchantops.domain.shared.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketCommandServiceTest {

    @Mock
    private TicketCommandPort ticketCommandPort;

    @Mock
    private TicketAuditPort ticketAuditPort;

    @Test
    void createTicketShouldCreateOpenTicketAndRecordAuditTrail() {
        TicketCommandService service = new TicketCommandService(ticketCommandPort, ticketAuditPort);
        when(ticketCommandPort.createTicket(any(NewTicketDraft.class))).thenReturn(new ManagedTicket(
                88L,
                1L,
                "POS printer offline",
                "Store printer stopped responding",
                "OPEN",
                null,
                101L,
                "ticket-create-req-1",
                LocalDateTime.of(2026, 3, 11, 10, 0),
                LocalDateTime.of(2026, 3, 11, 10, 0)
        ));

        TicketWriteResult result = service.createTicket(
                1L,
                101L,
                "ticket-create-req-1",
                new CreateTicketCommand(" POS printer offline ", " Store printer stopped responding ")
        );

        ArgumentCaptor<NewTicketDraft> ticketCaptor = ArgumentCaptor.forClass(NewTicketDraft.class);
        verify(ticketCommandPort).createTicket(ticketCaptor.capture());
        assertThat(ticketCaptor.getValue().title()).isEqualTo("POS printer offline");
        assertThat(ticketCaptor.getValue().description()).isEqualTo("Store printer stopped responding");
        assertThat(ticketCaptor.getValue().status()).isEqualTo("OPEN");
        verify(ticketCommandPort).appendOperationLog(any(NewTicketOperationLogDraft.class));
        verify(ticketAuditPort).recordEvent(eq(1L), eq("TICKET"), eq(88L), eq("TICKET_CREATED"), eq(101L), eq("ticket-create-req-1"), eq(null), any());
        assertThat(result.id()).isEqualTo(88L);
        assertThat(result.status()).isEqualTo("OPEN");
    }

    @Test
    void assignTicketShouldRejectInactiveAssignee() {
        TicketCommandService service = new TicketCommandService(ticketCommandPort, ticketAuditPort);
        when(ticketCommandPort.findManagedTicket(1L, 11L)).thenReturn(Optional.of(ticket(11L, 1L, "OPEN", null)));
        when(ticketCommandPort.findTenantUser(1L, 102L)).thenReturn(Optional.of(new TicketUserAccount(102L, "ops", "DISABLED")));

        assertThatThrownBy(() -> service.assignTicket(1L, 101L, "assign-req-1", 11L, new AssignTicketCommand(102L)))
                .isInstanceOf(BizException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BAD_REQUEST);
    }

    @Test
    void updateStatusShouldAllowReopenFromClosedToOpen() {
        TicketCommandService service = new TicketCommandService(ticketCommandPort, ticketAuditPort);
        when(ticketCommandPort.findManagedTicket(1L, 11L)).thenReturn(Optional.of(ticket(11L, 1L, "CLOSED", 102L)));
        when(ticketCommandPort.saveTicket(any(TicketUpdateDraft.class))).thenAnswer(invocation -> {
            TicketUpdateDraft draft = invocation.getArgument(0);
            return new ManagedTicket(
                    draft.id(),
                    draft.tenantId(),
                    draft.title(),
                    draft.description(),
                    draft.status(),
                    draft.assigneeId(),
                    draft.createdBy(),
                    draft.requestId(),
                    draft.createdAt(),
                    draft.updatedAt()
            );
        });
        when(ticketCommandPort.findTenantUser(1L, 102L)).thenReturn(Optional.of(new TicketUserAccount(102L, "ops", "ACTIVE")));

        TicketWriteResult result = service.updateStatus(1L, 101L, "reopen-req-1", 11L, new UpdateTicketStatusCommand("OPEN"));

        ArgumentCaptor<TicketUpdateDraft> ticketCaptor = ArgumentCaptor.forClass(TicketUpdateDraft.class);
        verify(ticketCommandPort).saveTicket(ticketCaptor.capture());
        assertThat(ticketCaptor.getValue().status()).isEqualTo("OPEN");
        verify(ticketCommandPort).appendOperationLog(any(NewTicketOperationLogDraft.class));
        assertThat(result.status()).isEqualTo("OPEN");
        assertThat(result.assigneeUsername()).isEqualTo("ops");
    }

    @Test
    void addCommentShouldPersistCommentAndReturnOperatorUsername() {
        TicketCommandService service = new TicketCommandService(ticketCommandPort, ticketAuditPort);
        when(ticketCommandPort.findManagedTicket(1L, 11L)).thenReturn(Optional.of(ticket(11L, 1L, "IN_PROGRESS", 102L)));
        when(ticketCommandPort.findTenantUser(1L, 102L)).thenReturn(Optional.of(new TicketUserAccount(102L, "ops", "ACTIVE")));
        when(ticketCommandPort.createComment(any(NewTicketCommentDraft.class))).thenReturn(new StoredTicketComment(
                31L,
                11L,
                "Restarted the printer",
                102L,
                LocalDateTime.of(2026, 3, 11, 10, 10)
        ));
        when(ticketCommandPort.saveTicket(any(TicketUpdateDraft.class))).thenReturn(ticket(11L, 1L, "IN_PROGRESS", 102L));

        TicketCommentResult result = service.addComment(1L, 102L, "comment-req-1", 11L, new AddTicketCommentCommand("Restarted the printer"));

        verify(ticketCommandPort).appendOperationLog(any(NewTicketOperationLogDraft.class));
        verify(ticketAuditPort).recordEvent(eq(1L), eq("TICKET"), eq(11L), eq("TICKET_COMMENT_ADDED"), eq(102L), eq("comment-req-1"), eq(null), any());
        assertThat(result.id()).isEqualTo(31L);
        assertThat(result.createdByUsername()).isEqualTo("ops");
    }

    private ManagedTicket ticket(Long id, Long tenantId, String status, Long assigneeId) {
        return new ManagedTicket(
                id,
                tenantId,
                "POS printer offline",
                "desc",
                status,
                assigneeId,
                101L,
                "seed-ticket-" + id,
                LocalDateTime.of(2026, 3, 11, 10, 0),
                LocalDateTime.of(2026, 3, 11, 10, 5)
        );
    }
}
