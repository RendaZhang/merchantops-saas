package com.renda.merchantops.api.service;

import com.renda.merchantops.api.ai.TicketAiPromptContext;
import com.renda.merchantops.api.ai.TicketAiPromptSupport;
import com.renda.merchantops.api.ai.TicketSummaryPromptBuilder;
import com.renda.merchantops.api.dto.ticket.query.TicketAiInteractionPageQuery;
import com.renda.merchantops.api.dto.ticket.query.TicketAiInteractionPageResponse;
import com.renda.merchantops.api.dto.ticket.query.TicketDetailResponse;
import com.renda.merchantops.api.dto.ticket.query.TicketPageQuery;
import com.renda.merchantops.api.dto.ticket.query.TicketPageResponse;
import com.renda.merchantops.common.exception.BizException;
import com.renda.merchantops.common.exception.ErrorCode;
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
import org.mockito.InjectMocks;
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
    private AiInteractionRecordRepository aiInteractionRecordRepository;

    @Mock
    private TicketCommentRepository ticketCommentRepository;

    @Mock
    private TicketOperationLogRepository ticketOperationLogRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TicketQueryService ticketQueryService;

    @Test
    void pageTicketsShouldNormalizeFiltersAndMapAssigneeUsername() {
        TicketPageQuery query = new TicketPageQuery(-1, 999, " OPEN ", 102L, "  printer  ", null);
        TicketEntity ticket = ticket(11L, 1L, "POS printer offline", "OPEN", 102L, 101L);

        when(ticketRepository.pageByTenantAndFilters(eq(1L), eq("OPEN"), eq(102L), eq("printer"), eq(false), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ticket), PageRequest.of(0, 100), 1));
        when(userRepository.findAllByTenantIdAndIdIn(1L, java.util.Set.of(102L)))
                .thenReturn(List.of(user(102L, 1L, "ops")));

        TicketPageResponse response = ticketQueryService.pageTickets(1L, query);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(ticketRepository).pageByTenantAndFilters(eq(1L), eq("OPEN"), eq(102L), eq("printer"), eq(false), pageableCaptor.capture());

        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(100);
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getAssigneeUsername()).isEqualTo("ops");
        assertThat(response.getPage()).isEqualTo(0);
        assertThat(response.getSize()).isEqualTo(100);
        assertThat(response.getTotal()).isEqualTo(1);
    }

    @Test
    void pageTicketsShouldRejectAssigneeAndUnassignedOnlyCombination() {
        TicketPageQuery query = new TicketPageQuery(0, 10, null, 102L, null, true);

        assertThatThrownBy(() -> ticketQueryService.pageTickets(1L, query))
                .isInstanceOf(BizException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BAD_REQUEST);
    }

    @Test
    void pageTicketAiInteractionsShouldNormalizeFiltersAndMapStoredFields() {
        TicketEntity ticket = ticket(11L, 1L, "POS printer offline", "OPEN", 102L, 101L);
        TicketAiInteractionPageQuery query = new TicketAiInteractionPageQuery(-1, 999, " SUMMARY ", " SUCCEEDED ");
        AiInteractionRecordEntity record = aiInteractionRecord(
                9001L,
                1L,
                11L,
                "SUMMARY",
                "SUCCEEDED",
                "Issue: Printer cable replacement is in progress under ops.",
                "ticket-summary-v1",
                "gpt-4.1-mini",
                412L,
                "ticket-ai-summary-req-1",
                120,
                52,
                172,
                1900L,
                LocalDateTime.of(2026, 3, 22, 8, 30)
        );

        when(ticketRepository.findByIdAndTenantId(11L, 1L)).thenReturn(Optional.of(ticket));
        when(aiInteractionRecordRepository.searchPageByTenantIdAndEntity(
                eq(1L),
                eq("TICKET"),
                eq(11L),
                eq("SUMMARY"),
                eq("SUCCEEDED"),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(record), PageRequest.of(0, 100), 1));

        TicketAiInteractionPageResponse response = ticketQueryService.pageTicketAiInteractions(1L, 11L, query);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(aiInteractionRecordRepository).searchPageByTenantIdAndEntity(
                eq(1L),
                eq("TICKET"),
                eq(11L),
                eq("SUMMARY"),
                eq("SUCCEEDED"),
                pageableCaptor.capture()
        );

        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(100);
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("createdAt").getDirection()).isEqualTo(org.springframework.data.domain.Sort.Direction.DESC);
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("id").getDirection()).isEqualTo(org.springframework.data.domain.Sort.Direction.DESC);
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().getFirst().getId()).isEqualTo(9001L);
        assertThat(response.getItems().getFirst().getInteractionType()).isEqualTo("SUMMARY");
        assertThat(response.getItems().getFirst().getStatus()).isEqualTo("SUCCEEDED");
        assertThat(response.getItems().getFirst().getOutputSummary()).isEqualTo("Issue: Printer cable replacement is in progress under ops.");
        assertThat(response.getItems().getFirst().getPromptVersion()).isEqualTo("ticket-summary-v1");
        assertThat(response.getItems().getFirst().getModelId()).isEqualTo("gpt-4.1-mini");
        assertThat(response.getItems().getFirst().getLatencyMs()).isEqualTo(412L);
        assertThat(response.getItems().getFirst().getRequestId()).isEqualTo("ticket-ai-summary-req-1");
        assertThat(response.getItems().getFirst().getUsagePromptTokens()).isEqualTo(120);
        assertThat(response.getItems().getFirst().getUsageCompletionTokens()).isEqualTo(52);
        assertThat(response.getItems().getFirst().getUsageTotalTokens()).isEqualTo(172);
        assertThat(response.getItems().getFirst().getUsageCostMicros()).isEqualTo(1900L);
        assertThat(response.getPage()).isEqualTo(0);
        assertThat(response.getSize()).isEqualTo(100);
        assertThat(response.getTotal()).isEqualTo(1);
    }

    @Test
    void pageTicketAiInteractionsShouldThrowNotFoundWhenTicketMissing() {
        when(ticketRepository.findByIdAndTenantId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketQueryService.pageTicketAiInteractions(1L, 99L, new TicketAiInteractionPageQuery()))
                .isInstanceOf(BizException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOT_FOUND);
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

    @Test
    void getTicketPromptContextShouldWindowRecentHistoryAndPromptShouldMarkOmissionsAndTruncateFields() {
        TicketEntity ticket = ticket(11L, 1L, "POS printer offline", "IN_PROGRESS", 102L, 101L);
        ticket.setDescription("d".repeat(650));

        List<TicketCommentEntity> recentCommentsDesc = new ArrayList<>();
        for (long id = 121L; id >= 101L; id--) {
            recentCommentsDesc.add(comment(id, 1L, 11L, 102L, "c".repeat(320) + "-" + id));
        }
        List<TicketOperationLogEntity> recentLogsDesc = new ArrayList<>();
        for (long id = 221L; id >= 201L; id--) {
            recentLogsDesc.add(operationLog(id, 1L, 11L, 101L, "STATUS_CHANGED", "l".repeat(220) + "-" + id));
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

        TicketAiPromptContext context = ticketQueryService.getTicketPromptContext(1L, 11L);

        ArgumentCaptor<Pageable> commentPageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        ArgumentCaptor<Pageable> logPageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(ticketCommentRepository).findByTicketIdAndTenantIdOrderByIdDesc(eq(11L), eq(1L), commentPageableCaptor.capture());
        verify(ticketOperationLogRepository).findByTicketIdAndTenantIdOrderByIdDesc(eq(11L), eq(1L), logPageableCaptor.capture());

        assertThat(commentPageableCaptor.getValue().getPageSize()).isEqualTo(TicketAiPromptSupport.COMMENT_HISTORY_LIMIT + 1);
        assertThat(logPageableCaptor.getValue().getPageSize()).isEqualTo(TicketAiPromptSupport.OPERATION_LOG_HISTORY_LIMIT + 1);
        assertThat(context.comments()).hasSize(TicketAiPromptSupport.COMMENT_HISTORY_LIMIT);
        assertThat(context.comments().getFirst().id()).isEqualTo(102L);
        assertThat(context.comments().getLast().id()).isEqualTo(121L);
        assertThat(context.olderCommentsOmitted()).isTrue();
        assertThat(context.operationLogs()).hasSize(TicketAiPromptSupport.OPERATION_LOG_HISTORY_LIMIT);
        assertThat(context.operationLogs().getFirst().id()).isEqualTo(202L);
        assertThat(context.operationLogs().getLast().id()).isEqualTo(221L);
        assertThat(context.olderOperationLogsOmitted()).isTrue();

        String userPrompt = new TicketSummaryPromptBuilder().build("ticket-summary-v1", context).userPrompt();
        assertThat(userPrompt).contains("- earlier comments omitted");
        assertThat(userPrompt).contains("- earlier operation logs omitted");
        assertThat(userPrompt).contains("description: " + "d".repeat(597) + "...");
        assertThat(userPrompt).contains(": " + "c".repeat(297) + "...");
        assertThat(userPrompt).contains(": " + "l".repeat(197) + "...");
        assertThat(userPrompt).doesNotContain("d".repeat(650));
    }

    @Test
    void getTicketDetailShouldReturnFullHistoryEvenWhenAiPromptContextIsCapped() {
        TicketEntity ticket = ticket(11L, 1L, "POS printer offline", "IN_PROGRESS", 102L, 101L);
        List<TicketCommentEntity> commentsAsc = new ArrayList<>();
        for (long id = 101L; id <= 121L; id++) {
            commentsAsc.add(comment(id, 1L, 11L, 102L, "comment-" + id));
        }
        List<TicketOperationLogEntity> logsAsc = new ArrayList<>();
        for (long id = 201L; id <= 221L; id++) {
            logsAsc.add(operationLog(id, 1L, 11L, 101L, "STATUS_CHANGED", "log-" + id));
        }

        when(ticketRepository.findByIdAndTenantId(11L, 1L)).thenReturn(Optional.of(ticket));
        when(ticketCommentRepository.findAllByTicketIdAndTenantIdOrderByIdAsc(11L, 1L)).thenReturn(commentsAsc);
        when(ticketOperationLogRepository.findAllByTicketIdAndTenantIdOrderByIdAsc(11L, 1L)).thenReturn(logsAsc);
        when(userRepository.findAllByTenantIdAndIdIn(eq(1L), any()))
                .thenReturn(List.of(
                        user(101L, 1L, "admin"),
                        user(102L, 1L, "ops")
                ));

        TicketDetailResponse response = ticketQueryService.getTicketDetail(1L, 11L);

        assertThat(response.getComments()).hasSize(21);
        assertThat(response.getComments().getFirst().getId()).isEqualTo(101L);
        assertThat(response.getComments().getLast().getId()).isEqualTo(121L);
        assertThat(response.getOperationLogs()).hasSize(21);
        assertThat(response.getOperationLogs().getFirst().getId()).isEqualTo(201L);
        assertThat(response.getOperationLogs().getLast().getId()).isEqualTo(221L);
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
        ticket.setRequestId("req-1");
        ticket.setCreatedAt(LocalDateTime.of(2026, 3, 11, 10, 0));
        ticket.setUpdatedAt(LocalDateTime.of(2026, 3, 11, 10, 5));
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

    private AiInteractionRecordEntity aiInteractionRecord(Long id,
                                                          Long tenantId,
                                                          Long entityId,
                                                          String interactionType,
                                                          String status,
                                                          String outputSummary,
                                                          String promptVersion,
                                                          String modelId,
                                                          Long latencyMs,
                                                          String requestId,
                                                          Integer usagePromptTokens,
                                                          Integer usageCompletionTokens,
                                                          Integer usageTotalTokens,
                                                          Long usageCostMicros,
                                                          LocalDateTime createdAt) {
        AiInteractionRecordEntity record = new AiInteractionRecordEntity();
        record.setId(id);
        record.setTenantId(tenantId);
        record.setUserId(103L);
        record.setEntityType("TICKET");
        record.setEntityId(entityId);
        record.setInteractionType(interactionType);
        record.setStatus(status);
        record.setOutputSummary(outputSummary);
        record.setPromptVersion(promptVersion);
        record.setModelId(modelId);
        record.setLatencyMs(latencyMs);
        record.setRequestId(requestId);
        record.setUsagePromptTokens(usagePromptTokens);
        record.setUsageCompletionTokens(usageCompletionTokens);
        record.setUsageTotalTokens(usageTotalTokens);
        record.setUsageCostMicros(usageCostMicros);
        record.setCreatedAt(createdAt);
        return record;
    }
}
