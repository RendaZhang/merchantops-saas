package com.renda.merchantops.api.service;

import com.renda.merchantops.api.dto.ticket.command.TicketAssigneeUpdateCommand;
import com.renda.merchantops.api.dto.ticket.command.TicketCommentCreateCommand;
import com.renda.merchantops.api.dto.ticket.command.TicketCreateCommand;
import com.renda.merchantops.api.dto.ticket.command.TicketStatusUpdateCommand;
import com.renda.merchantops.api.dto.ticket.command.TicketWriteResponse;
import com.renda.merchantops.api.dto.ticket.query.TicketCommentResponse;
import com.renda.merchantops.common.exception.BizException;
import com.renda.merchantops.common.exception.ErrorCode;
import com.renda.merchantops.infra.persistence.entity.TicketCommentEntity;
import com.renda.merchantops.infra.persistence.entity.TicketEntity;
import com.renda.merchantops.infra.persistence.entity.TicketOperationLogEntity;
import com.renda.merchantops.infra.persistence.entity.UserEntity;
import com.renda.merchantops.infra.repository.TicketCommentRepository;
import com.renda.merchantops.infra.repository.TicketOperationLogRepository;
import com.renda.merchantops.infra.repository.TicketRepository;
import com.renda.merchantops.infra.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketCommandServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private TicketCommentRepository ticketCommentRepository;

    @Mock
    private TicketOperationLogRepository ticketOperationLogRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TicketCommandService ticketCommandService;

    @Test
    void createTicketShouldPersistOpenTicketAndCreatedLog() {
        when(ticketRepository.save(any(TicketEntity.class))).thenAnswer(invocation -> {
            TicketEntity ticket = invocation.getArgument(0);
            ticket.setId(88L);
            return ticket;
        });

        TicketWriteResponse response = ticketCommandService.createTicket(
                1L,
                101L,
                "ticket-create-req-1",
                new TicketCreateCommand("POS printer offline", "Store printer stopped responding")
        );

        assertThat(response.getId()).isEqualTo(88L);
        assertThat(response.getStatus()).isEqualTo("OPEN");
        assertThat(response.getAssigneeId()).isNull();

        ArgumentCaptor<TicketEntity> ticketCaptor = ArgumentCaptor.forClass(TicketEntity.class);
        verify(ticketRepository).save(ticketCaptor.capture());
        assertThat(ticketCaptor.getValue().getTenantId()).isEqualTo(1L);
        assertThat(ticketCaptor.getValue().getTitle()).isEqualTo("POS printer offline");
        assertThat(ticketCaptor.getValue().getCreatedBy()).isEqualTo(101L);
        assertThat(ticketCaptor.getValue().getRequestId()).isEqualTo("ticket-create-req-1");

        ArgumentCaptor<TicketOperationLogEntity> logCaptor = ArgumentCaptor.forClass(TicketOperationLogEntity.class);
        verify(ticketOperationLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getOperationType()).isEqualTo("CREATED");
        assertThat(logCaptor.getValue().getDetail()).isEqualTo("ticket created");
        assertThat(logCaptor.getValue().getRequestId()).isEqualTo("ticket-create-req-1");
    }

    @Test
    void assignTicketShouldRejectAssigneeOutsideCurrentTenant() {
        when(ticketRepository.findByIdAndTenantId(11L, 1L)).thenReturn(Optional.of(ticket(11L, 1L, "OPEN", null)));
        when(userRepository.findByIdAndTenantId(201L, 1L)).thenReturn(Optional.empty());

        assertBizException(
                () -> ticketCommandService.assignTicket(1L, 101L, "assign-req-1", 11L, new TicketAssigneeUpdateCommand(201L)),
                ErrorCode.BAD_REQUEST,
                "user must exist in current tenant"
        );
    }

    @Test
    void assignTicketShouldPersistAssigneeAndAssignedLog() {
        TicketEntity ticket = ticket(11L, 1L, "OPEN", null);
        UserEntity assignee = user(102L, 1L, "ops", "ACTIVE");
        when(ticketRepository.findByIdAndTenantId(11L, 1L)).thenReturn(Optional.of(ticket));
        when(userRepository.findByIdAndTenantId(102L, 1L)).thenReturn(Optional.of(assignee));
        when(ticketRepository.save(any(TicketEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TicketWriteResponse response = ticketCommandService.assignTicket(
                1L,
                101L,
                "assign-req-1",
                11L,
                new TicketAssigneeUpdateCommand(102L)
        );

        assertThat(response.getAssigneeId()).isEqualTo(102L);
        assertThat(response.getAssigneeUsername()).isEqualTo("ops");
        assertThat(ticket.getUpdatedAt()).isNotNull();

        ArgumentCaptor<TicketOperationLogEntity> logCaptor = ArgumentCaptor.forClass(TicketOperationLogEntity.class);
        verify(ticketOperationLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getOperationType()).isEqualTo("ASSIGNED");
        assertThat(logCaptor.getValue().getDetail()).isEqualTo("assigned to ops");
    }

    @Test
    void updateStatusShouldAllowReopenFromClosedToOpen() {
        TicketEntity ticket = ticket(11L, 1L, "CLOSED", 102L);
        when(ticketRepository.findByIdAndTenantId(11L, 1L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(TicketEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findByIdAndTenantId(102L, 1L)).thenReturn(Optional.of(user(102L, 1L, "ops", "ACTIVE")));

        TicketWriteResponse response = ticketCommandService.updateStatus(
                1L,
                101L,
                "reopen-req-1",
                11L,
                new TicketStatusUpdateCommand("OPEN")
        );

        assertThat(response.getStatus()).isEqualTo("OPEN");
        assertThat(response.getAssigneeId()).isEqualTo(102L);
        assertThat(ticket.getUpdatedAt()).isNotNull();

        ArgumentCaptor<TicketOperationLogEntity> logCaptor = ArgumentCaptor.forClass(TicketOperationLogEntity.class);
        verify(ticketOperationLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getOperationType()).isEqualTo("STATUS_CHANGED");
        assertThat(logCaptor.getValue().getDetail()).isEqualTo("status changed from CLOSED to OPEN");
    }

    @Test
    void updateStatusShouldRejectIllegalTransitionFromInProgressToOpen() {
        when(ticketRepository.findByIdAndTenantId(11L, 1L)).thenReturn(Optional.of(ticket(11L, 1L, "IN_PROGRESS", 102L)));

        assertBizException(
                () -> ticketCommandService.updateStatus(1L, 101L, "status-req-1", 11L, new TicketStatusUpdateCommand("OPEN")),
                ErrorCode.BAD_REQUEST,
                "ticket status transition is not allowed"
        );
    }

    @Test
    void updateStatusShouldRejectNoopTransition() {
        when(ticketRepository.findByIdAndTenantId(11L, 1L)).thenReturn(Optional.of(ticket(11L, 1L, "CLOSED", 102L)));

        assertBizException(
                () -> ticketCommandService.updateStatus(1L, 101L, "status-req-1", 11L, new TicketStatusUpdateCommand("CLOSED")),
                ErrorCode.BAD_REQUEST,
                "ticket is already in status CLOSED"
        );
    }

    @Test
    void addCommentShouldPersistCommentRefreshTicketAndWriteCommentLog() {
        TicketEntity ticket = ticket(11L, 1L, "IN_PROGRESS", 102L);
        UserEntity operator = user(102L, 1L, "ops", "ACTIVE");
        when(ticketRepository.findByIdAndTenantId(11L, 1L)).thenReturn(Optional.of(ticket));
        when(userRepository.findByIdAndTenantId(102L, 1L)).thenReturn(Optional.of(operator));
        when(ticketCommentRepository.save(any(TicketCommentEntity.class))).thenAnswer(invocation -> {
            TicketCommentEntity comment = invocation.getArgument(0);
            comment.setId(31L);
            return comment;
        });
        when(ticketRepository.save(any(TicketEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TicketCommentResponse response = ticketCommandService.addComment(
                1L,
                102L,
                "comment-req-1",
                11L,
                new TicketCommentCreateCommand("Restarted the printer")
        );

        assertThat(response.getId()).isEqualTo(31L);
        assertThat(response.getCreatedByUsername()).isEqualTo("ops");
        assertThat(ticket.getUpdatedAt()).isNotNull();

        ArgumentCaptor<TicketCommentEntity> commentCaptor = ArgumentCaptor.forClass(TicketCommentEntity.class);
        verify(ticketCommentRepository).save(commentCaptor.capture());
        assertThat(commentCaptor.getValue().getRequestId()).isEqualTo("comment-req-1");
        assertThat(commentCaptor.getValue().getContent()).isEqualTo("Restarted the printer");

        ArgumentCaptor<TicketOperationLogEntity> logCaptor = ArgumentCaptor.forClass(TicketOperationLogEntity.class);
        verify(ticketOperationLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getOperationType()).isEqualTo("COMMENTED");
        assertThat(logCaptor.getValue().getDetail()).isEqualTo("comment added");
    }

    private void assertBizException(ThrowingCall call, ErrorCode errorCode, String message) {
        assertThatThrownBy(call::run)
                .isInstanceOf(BizException.class)
                .satisfies(ex -> {
                    BizException bizException = (BizException) ex;
                    assertThat(bizException.getErrorCode()).isEqualTo(errorCode);
                    assertThat(bizException.getMessage()).isEqualTo(message);
                });
    }

    @FunctionalInterface
    private interface ThrowingCall {
        void run();
    }

    private TicketEntity ticket(Long id, Long tenantId, String status, Long assigneeId) {
        TicketEntity ticket = new TicketEntity();
        ticket.setId(id);
        ticket.setTenantId(tenantId);
        ticket.setTitle("POS printer offline");
        ticket.setDescription("desc");
        ticket.setStatus(status);
        ticket.setAssigneeId(assigneeId);
        ticket.setCreatedBy(101L);
        ticket.setRequestId("seed-ticket-" + id);
        ticket.setCreatedAt(LocalDateTime.of(2026, 3, 11, 10, 0));
        ticket.setUpdatedAt(LocalDateTime.of(2026, 3, 11, 10, 5));
        return ticket;
    }

    private UserEntity user(Long id, Long tenantId, String username, String status) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setTenantId(tenantId);
        user.setUsername(username);
        user.setStatus(status);
        return user;
    }
}
