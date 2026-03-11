package com.renda.merchantops.api.service;

import com.renda.merchantops.api.dto.ticket.query.TicketDetailResponse;
import com.renda.merchantops.api.dto.ticket.query.TicketPageQuery;
import com.renda.merchantops.api.dto.ticket.query.TicketPageResponse;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketQueryServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private TicketCommentRepository ticketCommentRepository;

    @Mock
    private TicketOperationLogRepository ticketOperationLogRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TicketQueryService ticketQueryService;

    @Test
    void pageTicketsShouldNormalizeStatusFilterAndMapAssigneeUsername() {
        TicketPageQuery query = new TicketPageQuery(-1, 999, " OPEN ");
        TicketEntity ticket = ticket(11L, 1L, "POS printer offline", "OPEN", 102L, 101L);

        when(ticketRepository.findAllByTenantIdAndStatus(eq(1L), eq("OPEN"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ticket), PageRequest.of(0, 100), 1));
        when(userRepository.findAllByTenantIdAndIdIn(1L, java.util.Set.of(102L)))
                .thenReturn(List.of(user(102L, 1L, "ops")));

        TicketPageResponse response = ticketQueryService.pageTickets(1L, query);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(ticketRepository).findAllByTenantIdAndStatus(eq(1L), eq("OPEN"), pageableCaptor.capture());

        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(100);
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getAssigneeUsername()).isEqualTo("ops");
        assertThat(response.getPage()).isEqualTo(0);
        assertThat(response.getSize()).isEqualTo(100);
        assertThat(response.getTotal()).isEqualTo(1);
    }

    @Test
    void getTicketDetailShouldMapCommentsAndOperationLogs() {
        TicketEntity ticket = ticket(11L, 1L, "POS printer offline", "IN_PROGRESS", 102L, 101L);
        TicketCommentEntity comment = comment(31L, 1L, 11L, 102L, "Printer restarted");
        TicketOperationLogEntity createdLog = operationLog(41L, 1L, 11L, 101L, "CREATED", "ticket created");
        TicketOperationLogEntity assignedLog = operationLog(42L, 1L, 11L, 101L, "ASSIGNED", "assigned to ops");

        when(ticketRepository.findByIdAndTenantId(11L, 1L)).thenReturn(Optional.of(ticket));
        when(ticketCommentRepository.findAllByTicketIdAndTenantIdOrderByIdAsc(11L, 1L)).thenReturn(List.of(comment));
        when(ticketOperationLogRepository.findAllByTicketIdAndTenantIdOrderByIdAsc(11L, 1L))
                .thenReturn(List.of(createdLog, assignedLog));
        when(userRepository.findAllByTenantIdAndIdIn(eq(1L), any()))
                .thenReturn(List.of(
                        user(101L, 1L, "admin"),
                        user(102L, 1L, "ops")
                ));

        TicketDetailResponse response = ticketQueryService.getTicketDetail(1L, 11L);

        assertThat(response.getId()).isEqualTo(11L);
        assertThat(response.getAssigneeUsername()).isEqualTo("ops");
        assertThat(response.getCreatedByUsername()).isEqualTo("admin");
        assertThat(response.getComments()).hasSize(1);
        assertThat(response.getComments().get(0).getCreatedByUsername()).isEqualTo("ops");
        assertThat(response.getOperationLogs()).hasSize(2);
        assertThat(response.getOperationLogs().get(1).getDetail()).isEqualTo("assigned to ops");
    }

    @Test
    void getTicketDetailShouldThrowNotFoundWhenTicketMissing() {
        when(ticketRepository.findByIdAndTenantId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketQueryService.getTicketDetail(1L, 99L))
                .isInstanceOf(BizException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    private TicketEntity ticket(Long id,
                                Long tenantId,
                                String title,
                                String status,
                                Long assigneeId,
                                Long createdBy) {
        TicketEntity ticket = new TicketEntity();
        ticket.setId(id);
        ticket.setTenantId(tenantId);
        ticket.setTitle(title);
        ticket.setDescription("desc");
        ticket.setStatus(status);
        ticket.setAssigneeId(assigneeId);
        ticket.setCreatedBy(createdBy);
        ticket.setRequestId("req-" + id);
        ticket.setCreatedAt(LocalDateTime.of(2026, 3, 11, 10, 0));
        ticket.setUpdatedAt(LocalDateTime.of(2026, 3, 11, 10, 30));
        return ticket;
    }

    private TicketCommentEntity comment(Long id,
                                        Long tenantId,
                                        Long ticketId,
                                        Long createdBy,
                                        String content) {
        TicketCommentEntity comment = new TicketCommentEntity();
        comment.setId(id);
        comment.setTenantId(tenantId);
        comment.setTicketId(ticketId);
        comment.setCreatedBy(createdBy);
        comment.setContent(content);
        comment.setRequestId("comment-" + id);
        comment.setCreatedAt(LocalDateTime.of(2026, 3, 11, 10, 15));
        return comment;
    }

    private TicketOperationLogEntity operationLog(Long id,
                                                  Long tenantId,
                                                  Long ticketId,
                                                  Long operatorId,
                                                  String operationType,
                                                  String detail) {
        TicketOperationLogEntity log = new TicketOperationLogEntity();
        log.setId(id);
        log.setTenantId(tenantId);
        log.setTicketId(ticketId);
        log.setOperatorId(operatorId);
        log.setOperationType(operationType);
        log.setDetail(detail);
        log.setRequestId("log-" + id);
        log.setCreatedAt(LocalDateTime.of(2026, 3, 11, 10, 20));
        return log;
    }

    private UserEntity user(Long id, Long tenantId, String username) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setTenantId(tenantId);
        user.setUsername(username);
        return user;
    }
}
