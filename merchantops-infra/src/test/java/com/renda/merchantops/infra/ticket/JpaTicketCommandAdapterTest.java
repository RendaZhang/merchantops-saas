package com.renda.merchantops.infra.ticket;

import com.renda.merchantops.domain.ticket.NewTicketCommentDraft;
import com.renda.merchantops.domain.ticket.NewTicketDraft;
import com.renda.merchantops.domain.ticket.NewTicketOperationLogDraft;
import com.renda.merchantops.infra.persistence.entity.TicketCommentEntity;
import com.renda.merchantops.infra.persistence.entity.TicketEntity;
import com.renda.merchantops.infra.persistence.entity.TicketOperationLogEntity;
import com.renda.merchantops.infra.repository.TicketCommentRepository;
import com.renda.merchantops.infra.repository.TicketOperationLogRepository;
import com.renda.merchantops.infra.repository.TicketRepository;
import com.renda.merchantops.infra.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaTicketCommandAdapterTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private TicketCommentRepository ticketCommentRepository;

    @Mock
    private TicketOperationLogRepository ticketOperationLogRepository;

    @Mock
    private UserRepository userRepository;

    @Test
    void createTicketShouldMapDraftIntoEntity() {
        JpaTicketCommandAdapter adapter = new JpaTicketCommandAdapter(
                ticketRepository,
                ticketCommentRepository,
                ticketOperationLogRepository,
                userRepository
        );
        when(ticketRepository.save(org.mockito.ArgumentMatchers.any(TicketEntity.class))).thenAnswer(invocation -> {
            TicketEntity entity = invocation.getArgument(0);
            entity.setId(88L);
            return entity;
        });

        var result = adapter.createTicket(new NewTicketDraft(
                1L,
                "POS printer offline",
                "Store printer stopped responding",
                "OPEN",
                101L,
                "ticket-create-req-1",
                LocalDateTime.of(2026, 3, 11, 10, 0),
                LocalDateTime.of(2026, 3, 11, 10, 0)
        ));

        ArgumentCaptor<TicketEntity> entityCaptor = ArgumentCaptor.forClass(TicketEntity.class);
        verify(ticketRepository).save(entityCaptor.capture());
        assertThat(entityCaptor.getValue().getTitle()).isEqualTo("POS printer offline");
        assertThat(entityCaptor.getValue().getStatus()).isEqualTo("OPEN");
        assertThat(entityCaptor.getValue().getRequestId()).isEqualTo("ticket-create-req-1");
        assertThat(result.id()).isEqualTo(88L);
        assertThat(result.createdBy()).isEqualTo(101L);
    }

    @Test
    void createCommentShouldPersistDraftFields() {
        JpaTicketCommandAdapter adapter = new JpaTicketCommandAdapter(
                ticketRepository,
                ticketCommentRepository,
                ticketOperationLogRepository,
                userRepository
        );
        when(ticketCommentRepository.save(org.mockito.ArgumentMatchers.any(TicketCommentEntity.class))).thenAnswer(invocation -> {
            TicketCommentEntity entity = invocation.getArgument(0);
            entity.setId(31L);
            return entity;
        });

        var result = adapter.createComment(new NewTicketCommentDraft(
                1L,
                11L,
                "Restarted the printer",
                102L,
                "comment-req-1",
                LocalDateTime.of(2026, 3, 11, 10, 10)
        ));

        ArgumentCaptor<TicketCommentEntity> entityCaptor = ArgumentCaptor.forClass(TicketCommentEntity.class);
        verify(ticketCommentRepository).save(entityCaptor.capture());
        assertThat(entityCaptor.getValue().getContent()).isEqualTo("Restarted the printer");
        assertThat(entityCaptor.getValue().getRequestId()).isEqualTo("comment-req-1");
        assertThat(result.id()).isEqualTo(31L);
        assertThat(result.ticketId()).isEqualTo(11L);
    }

    @Test
    void appendOperationLogShouldPersistRequestedFields() {
        JpaTicketCommandAdapter adapter = new JpaTicketCommandAdapter(
                ticketRepository,
                ticketCommentRepository,
                ticketOperationLogRepository,
                userRepository
        );

        adapter.appendOperationLog(new NewTicketOperationLogDraft(
                11L,
                1L,
                101L,
                "assign-req-1",
                "ASSIGNED",
                "assigned to ops",
                LocalDateTime.of(2026, 3, 11, 10, 5)
        ));

        ArgumentCaptor<TicketOperationLogEntity> entityCaptor = ArgumentCaptor.forClass(TicketOperationLogEntity.class);
        verify(ticketOperationLogRepository).save(entityCaptor.capture());
        assertThat(entityCaptor.getValue().getTicketId()).isEqualTo(11L);
        assertThat(entityCaptor.getValue().getOperationType()).isEqualTo("ASSIGNED");
        assertThat(entityCaptor.getValue().getDetail()).isEqualTo("assigned to ops");
        assertThat(entityCaptor.getValue().getRequestId()).isEqualTo("assign-req-1");
    }
}
