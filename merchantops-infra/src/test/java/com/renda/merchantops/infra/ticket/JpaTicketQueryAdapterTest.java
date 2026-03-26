package com.renda.merchantops.infra.ticket;

import com.renda.merchantops.domain.ticket.TicketPromptContext;
import com.renda.merchantops.domain.ticket.TicketPromptWindowPolicy;
import com.renda.merchantops.infra.persistence.entity.AiInteractionRecordEntity;
import com.renda.merchantops.infra.persistence.entity.TicketCommentEntity;
import com.renda.merchantops.infra.persistence.entity.TicketEntity;
import com.renda.merchantops.infra.persistence.entity.TicketOperationLogEntity;
import com.renda.merchantops.infra.persistence.entity.UserEntity;
import com.renda.merchantops.infra.repository.AiInteractionRecordRepository;
import com.renda.merchantops.infra.repository.TicketCommentRepository;
import com.renda.merchantops.infra.repository.TicketOperationLogRepository;
import com.renda.merchantops.infra.repository.TicketRepository;
import com.renda.merchantops.infra.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaTicketQueryAdapterTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private AiInteractionRecordRepository aiInteractionRecordRepository;

    @Mock
    private TicketCommentRepository ticketCommentRepository;

    @Mock
    private TicketOperationLogRepository ticketOperationLogRepository;

    @Mock
    private UserRepository userRepository;

    @Test
    void pageTicketsShouldAllowUnassignedItemsWithoutUsernameLookupFailure() {
        JpaTicketQueryAdapter adapter = new JpaTicketQueryAdapter(
                ticketRepository,
                aiInteractionRecordRepository,
                ticketCommentRepository,
                ticketOperationLogRepository,
                userRepository
        );
        TicketEntity unassignedTicket = ticket(11L, 1L, "OPEN", null, 101L);
        when(ticketRepository.pageByTenantAndFilters(
                eq(1L), eq("OPEN"), eq(null), eq(null), eq(true), any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(unassignedTicket), PageRequest.of(0, 1), 1));

        var result = adapter.pageTickets(
                1L,
                new com.renda.merchantops.domain.ticket.TicketPageCriteria(0, 1, "OPEN", null, null, true)
        );

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().getFirst().id()).isEqualTo(11L);
        assertThat(result.items().getFirst().assigneeId()).isNull();
        assertThat(result.items().getFirst().assigneeUsername()).isNull();
    }

    @Test
    void findTicketPromptContextShouldWindowRecentHistoryAndMapUsernames() {
        JpaTicketQueryAdapter adapter = new JpaTicketQueryAdapter(
                ticketRepository,
                aiInteractionRecordRepository,
                ticketCommentRepository,
                ticketOperationLogRepository,
                userRepository
        );
        TicketEntity ticket = ticket(11L, 1L, "IN_PROGRESS", 102L, 101L);
        List<TicketCommentEntity> recentCommentsDesc = new ArrayList<>();
        for (long id = 121L; id >= 101L; id--) {
            recentCommentsDesc.add(comment(id, 1L, 11L, 102L, "comment-" + id));
        }
        List<TicketOperationLogEntity> recentLogsDesc = new ArrayList<>();
        for (long id = 221L; id >= 201L; id--) {
            recentLogsDesc.add(operationLog(id, 1L, 11L, 101L, "STATUS_CHANGED", "log-" + id));
        }

        when(ticketRepository.findByIdAndTenantId(11L, 1L)).thenReturn(Optional.of(ticket));
        when(ticketCommentRepository.findByTicketIdAndTenantIdOrderByIdDesc(eq(11L), eq(1L), any(Pageable.class)))
                .thenReturn(recentCommentsDesc);
        when(ticketOperationLogRepository.findByTicketIdAndTenantIdOrderByIdDesc(eq(11L), eq(1L), any(Pageable.class)))
                .thenReturn(recentLogsDesc);
        when(userRepository.findAllByTenantIdAndIdIn(eq(1L), any()))
                .thenReturn(List.of(
                        user(101L, 1L, "admin"),
                        user(102L, 1L, "ops")
                ));

        TicketPromptContext context = adapter.findTicketPromptContext(1L, 11L).orElseThrow();

        ArgumentCaptor<Pageable> commentPageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        ArgumentCaptor<Pageable> logPageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(ticketCommentRepository).findByTicketIdAndTenantIdOrderByIdDesc(eq(11L), eq(1L), commentPageableCaptor.capture());
        verify(ticketOperationLogRepository).findByTicketIdAndTenantIdOrderByIdDesc(eq(11L), eq(1L), logPageableCaptor.capture());
        assertThat(commentPageableCaptor.getValue().getPageSize()).isEqualTo(TicketPromptWindowPolicy.COMMENT_HISTORY_LIMIT + 1);
        assertThat(logPageableCaptor.getValue().getPageSize()).isEqualTo(TicketPromptWindowPolicy.OPERATION_LOG_HISTORY_LIMIT + 1);
        assertThat(context.assigneeUsername()).isEqualTo("ops");
        assertThat(context.createdByUsername()).isEqualTo("admin");
        assertThat(context.comments()).hasSize(TicketPromptWindowPolicy.COMMENT_HISTORY_LIMIT);
        assertThat(context.comments().getFirst().id()).isEqualTo(102L);
        assertThat(context.comments().getLast().id()).isEqualTo(121L);
        assertThat(context.olderCommentsOmitted()).isTrue();
        assertThat(context.operationLogs()).hasSize(TicketPromptWindowPolicy.OPERATION_LOG_HISTORY_LIMIT);
        assertThat(context.operationLogs().getFirst().id()).isEqualTo(202L);
        assertThat(context.operationLogs().getLast().id()).isEqualTo(221L);
        assertThat(context.olderOperationLogsOmitted()).isTrue();
    }

    @Test
    void pageTicketAiInteractionsShouldMapUsageFields() {
        JpaTicketQueryAdapter adapter = new JpaTicketQueryAdapter(
                ticketRepository,
                aiInteractionRecordRepository,
                ticketCommentRepository,
                ticketOperationLogRepository,
                userRepository
        );
        AiInteractionRecordEntity record = new AiInteractionRecordEntity();
        record.setId(9001L);
        record.setInteractionType("SUMMARY");
        record.setStatus("SUCCEEDED");
        record.setOutputSummary("Issue: Printer cable replacement is in progress under ops.");
        record.setPromptVersion("ticket-summary-v1");
        record.setModelId("gpt-4.1-mini");
        record.setLatencyMs(412L);
        record.setRequestId("ticket-ai-summary-req-1");
        record.setUsagePromptTokens(120);
        record.setUsageCompletionTokens(52);
        record.setUsageTotalTokens(172);
        record.setUsageCostMicros(1900L);
        record.setCreatedAt(LocalDateTime.of(2026, 3, 22, 8, 30));
        when(aiInteractionRecordRepository.searchPageByTenantIdAndEntity(
                eq(1L), eq("TICKET"), eq(11L), eq("SUMMARY"), eq("SUCCEEDED"), any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(record), PageRequest.of(0, 100), 1));

        var result = adapter.pageTicketAiInteractions(
                1L,
                11L,
                new com.renda.merchantops.domain.ticket.TicketAiInteractionPageCriteria(0, 100, "SUMMARY", "SUCCEEDED")
        );

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().getFirst().id()).isEqualTo(9001L);
        assertThat(result.items().getFirst().usageTotalTokens()).isEqualTo(172);
        assertThat(result.items().getFirst().usageCostMicros()).isEqualTo(1900L);
        assertThat(result.page()).isEqualTo(0);
        assertThat(result.size()).isEqualTo(100);
    }

    private TicketEntity ticket(Long id, Long tenantId, String status, Long assigneeId, Long createdBy) {
        TicketEntity ticket = new TicketEntity();
        ticket.setId(id);
        ticket.setTenantId(tenantId);
        ticket.setTitle("POS printer offline");
        ticket.setDescription("desc");
        ticket.setStatus(status);
        ticket.setAssigneeId(assigneeId);
        ticket.setCreatedBy(createdBy);
        ticket.setRequestId("req-1");
        ticket.setCreatedAt(LocalDateTime.of(2026, 3, 11, 10, 0));
        ticket.setUpdatedAt(LocalDateTime.of(2026, 3, 11, 10, 5));
        return ticket;
    }

    private TicketCommentEntity comment(Long id, Long tenantId, Long ticketId, Long createdBy, String content) {
        TicketCommentEntity comment = new TicketCommentEntity();
        comment.setId(id);
        comment.setTenantId(tenantId);
        comment.setTicketId(ticketId);
        comment.setContent(content);
        comment.setCreatedBy(createdBy);
        comment.setRequestId("req-c");
        comment.setCreatedAt(LocalDateTime.of(2026, 3, 11, 10, 10));
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
        log.setOperationType(operationType);
        log.setDetail(detail);
        log.setOperatorId(operatorId);
        log.setRequestId("req-l");
        log.setCreatedAt(LocalDateTime.of(2026, 3, 11, 10, 0));
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
