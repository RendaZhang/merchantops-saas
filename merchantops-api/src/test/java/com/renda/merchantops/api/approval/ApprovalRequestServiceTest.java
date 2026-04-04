package com.renda.merchantops.api.approval;

import com.renda.merchantops.api.audit.AuditEventService;
import com.renda.merchantops.api.dto.approval.query.ApprovalRequestPageQuery;
import com.renda.merchantops.api.dto.importjob.command.ImportJobSelectiveReplayProposalRequest;
import com.renda.merchantops.api.dto.ticket.command.TicketCommentProposalRequest;
import com.renda.merchantops.domain.approval.ImportSelectiveReplayApprovalCommand;
import com.renda.merchantops.domain.approval.ApprovalRequestPageCriteria;
import com.renda.merchantops.domain.approval.ApprovalRequestPageResult;
import com.renda.merchantops.domain.approval.ApprovalRequestRecord;
import com.renda.merchantops.domain.approval.ApprovalRequestUseCase;
import com.renda.merchantops.domain.approval.TicketCommentApprovalCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApprovalRequestServiceTest {

    @Mock
    private ApprovalRequestUseCase approvalRequestUseCase;

    @Mock
    private AuditEventService auditEventService;

    private ApprovalRequestCommandService approvalRequestCommandService;
    private ApprovalRequestQueryService approvalRequestQueryService;

    @BeforeEach
    void setUp() {
        ApprovalRequestResponseMapper mapper = new ApprovalRequestResponseMapper();
        approvalRequestCommandService = new ApprovalRequestCommandService(approvalRequestUseCase, auditEventService, mapper);
        approvalRequestQueryService = new ApprovalRequestQueryService(approvalRequestUseCase, mapper);
    }

    @Test
    void createDisableRequestShouldDelegateToUseCaseAndRecordAudit() {
        when(approvalRequestUseCase.createDisableRequest(1L, 101L, "disable-req-1", 103L))
                .thenReturn(record(901L, "PENDING", 103L, 101L, "USER_STATUS_DISABLE", "USER", "{\"status\":\"DISABLED\"}"));

        var response = approvalRequestCommandService.createDisableRequest(1L, 101L, "disable-req-1", 103L);

        assertThat(response.id()).isEqualTo(901L);
        assertThat(response.status()).isEqualTo("PENDING");
        verify(approvalRequestUseCase).createDisableRequest(1L, 101L, "disable-req-1", 103L);
        verify(auditEventService).recordEvent(eq(1L), eq("APPROVAL_REQUEST"), eq(901L), eq("APPROVAL_REQUEST_CREATED"), eq(101L), eq("disable-req-1"), eq(null), any());
    }

    @Test
    void createImportSelectiveReplayRequestShouldDelegateToUseCaseAndRecordAudit() {
        ImportJobSelectiveReplayProposalRequest request = new ImportJobSelectiveReplayProposalRequest(
                List.of("UNKNOWN_ROLE"),
                9103L,
                "Review role fixes before replay"
        );
        when(approvalRequestUseCase.createImportSelectiveReplayRequest(
                1L,
                101L,
                "proposal-req-1",
                new ImportSelectiveReplayApprovalCommand(7001L, List.of("UNKNOWN_ROLE"), 9103L, "Review role fixes before replay")
        )).thenReturn(record(902L, "PENDING", 7001L, 101L, "IMPORT_JOB_SELECTIVE_REPLAY", "IMPORT_JOB", "{\"sourceJobId\":7001,\"errorCodes\":[\"UNKNOWN_ROLE\"]}"));

        var response = approvalRequestCommandService.createImportSelectiveReplayRequest(1L, 101L, "proposal-req-1", 7001L, request);

        assertThat(response.id()).isEqualTo(902L);
        assertThat(response.actionType()).isEqualTo("IMPORT_JOB_SELECTIVE_REPLAY");
        verify(approvalRequestUseCase).createImportSelectiveReplayRequest(
                1L,
                101L,
                "proposal-req-1",
                new ImportSelectiveReplayApprovalCommand(7001L, List.of("UNKNOWN_ROLE"), 9103L, "Review role fixes before replay")
        );
        verify(auditEventService).recordEvent(eq(1L), eq("APPROVAL_REQUEST"), eq(902L), eq("APPROVAL_REQUEST_CREATED"), eq(101L), eq("proposal-req-1"), eq(null), any());
    }

    @Test
    void getByIdShouldMapDomainRecordToResponse() {
        when(approvalRequestUseCase.getById(1L, 901L)).thenReturn(record(901L, "PENDING", 103L, 101L, "USER_STATUS_DISABLE", "USER", "{\"status\":\"DISABLED\"}"));

        var response = approvalRequestQueryService.getById(1L, 901L, Set.of("USER_STATUS_DISABLE"));

        assertThat(response.id()).isEqualTo(901L);
        assertThat(response.entityId()).isEqualTo(103L);
        assertThat(response.requestedBy()).isEqualTo(101L);
    }

    @Test
    void pageShouldMapCriteriaAndDomainPage() {
        when(approvalRequestUseCase.page(eq(1L), any()))
                .thenReturn(new ApprovalRequestPageResult(List.of(record(901L, "PENDING", 103L, 101L, "USER_STATUS_DISABLE", "USER", "{\"status\":\"DISABLED\"}")), 0, 10, 1, 1));

        var response = approvalRequestQueryService.page(1L, new ApprovalRequestPageQuery(-1, 0, "  PENDING  ", " USER_STATUS_DISABLE ", 101L), Set.of("USER_STATUS_DISABLE", "TICKET_COMMENT_CREATE"));

        ArgumentCaptor<ApprovalRequestPageCriteria> criteriaCaptor = ArgumentCaptor.forClass(ApprovalRequestPageCriteria.class);
        verify(approvalRequestUseCase).page(eq(1L), criteriaCaptor.capture());
        assertThat(criteriaCaptor.getValue().page()).isEqualTo(-1);
        assertThat(criteriaCaptor.getValue().size()).isEqualTo(0);
        assertThat(criteriaCaptor.getValue().status()).isEqualTo("  PENDING  ");
        assertThat(criteriaCaptor.getValue().actionType()).isEqualTo(" USER_STATUS_DISABLE ");
        assertThat(criteriaCaptor.getValue().requestedBy()).isEqualTo(101L);
        assertThat(criteriaCaptor.getValue().allowedActionTypes()).containsExactlyInAnyOrder("USER_STATUS_DISABLE", "TICKET_COMMENT_CREATE");
        assertThat(response.getTotal()).isEqualTo(1);
        assertThat(response.getItems()).hasSize(1);
    }

    @Test
    void createTicketCommentRequestShouldDelegateToUseCaseAndRecordAudit() {
        TicketCommentProposalRequest request = new TicketCommentProposalRequest("Reply draft content", 9002L);
        when(approvalRequestUseCase.createTicketCommentRequest(
                1L,
                101L,
                "ticket-comment-proposal-1",
                new TicketCommentApprovalCommand(301L, "Reply draft content", 9002L)
        )).thenReturn(record(903L, "PENDING", 301L, 101L, "TICKET_COMMENT_CREATE", "TICKET", "{\"commentContent\":\"Reply draft content\",\"sourceInteractionId\":9002}"));

        var response = approvalRequestCommandService.createTicketCommentRequest(1L, 101L, "ticket-comment-proposal-1", 301L, request);

        assertThat(response.id()).isEqualTo(903L);
        assertThat(response.actionType()).isEqualTo("TICKET_COMMENT_CREATE");
        verify(approvalRequestUseCase).createTicketCommentRequest(
                1L,
                101L,
                "ticket-comment-proposal-1",
                new TicketCommentApprovalCommand(301L, "Reply draft content", 9002L)
        );
        verify(auditEventService).recordEvent(eq(1L), eq("APPROVAL_REQUEST"), eq(903L), eq("APPROVAL_REQUEST_CREATED"), eq(101L), eq("ticket-comment-proposal-1"), eq(null), any());
    }

    @Test
    void approveShouldDelegateAndEmitApprovalAndExecutionAuditEvents() {
        when(approvalRequestUseCase.approve(1L, 105L, "approve-req-1", 901L))
                .thenReturn(record(901L, "APPROVED", 103L, 101L, "USER_STATUS_DISABLE", "USER", "{\"status\":\"DISABLED\"}"));

        var response = approvalRequestCommandService.approve(1L, 105L, "approve-req-1", 901L);

        assertThat(response.status()).isEqualTo("APPROVED");
        verify(approvalRequestUseCase).approve(1L, 105L, "approve-req-1", 901L);
        verify(auditEventService, times(2)).recordEvent(eq(1L), eq("APPROVAL_REQUEST"), eq(901L), any(), eq(105L), eq("approve-req-1"), eq(null), any());
    }

    @Test
    void rejectShouldDelegateAndEmitRejectedAuditEvent() {
        when(approvalRequestUseCase.reject(1L, 105L, "reject-req-1", 901L))
                .thenReturn(record(901L, "REJECTED", 103L, 101L, "USER_STATUS_DISABLE", "USER", "{\"status\":\"DISABLED\"}"));

        var response = approvalRequestCommandService.reject(1L, 105L, "reject-req-1", 901L);

        assertThat(response.status()).isEqualTo("REJECTED");
        verify(approvalRequestUseCase).reject(1L, 105L, "reject-req-1", 901L);
        verify(auditEventService).recordEvent(eq(1L), eq("APPROVAL_REQUEST"), eq(901L), eq("APPROVAL_REQUEST_REJECTED"), eq(105L), eq("reject-req-1"), eq(null), any());
    }

    @Test
    void getByIdShouldHideActionOutsideAllowedSet() {
        when(approvalRequestUseCase.getById(1L, 901L))
                .thenReturn(record(901L, "PENDING", 103L, 101L, "USER_STATUS_DISABLE", "USER", "{\"status\":\"DISABLED\"}"));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> approvalRequestQueryService.getById(1L, 901L, Set.of("TICKET_COMMENT_CREATE")))
                .hasMessage("approval request not found");
    }

    private ApprovalRequestRecord record(Long id,
                                         String status,
                                         Long entityId,
                                         Long requestedBy,
                                         String actionType,
                                         String entityType,
                                         String payloadJson) {
        return new ApprovalRequestRecord(
                id,
                1L,
                actionType,
                entityType,
                entityId,
                requestedBy,
                "APPROVED".equals(status) ? 105L : null,
                status,
                payloadJson,
                "PENDING".equals(status) ? "pending-key" : null,
                "disable-req-1",
                LocalDateTime.of(2026, 3, 26, 10, 0),
                "PENDING".equals(status) ? null : LocalDateTime.of(2026, 3, 26, 10, 5),
                "APPROVED".equals(status) ? LocalDateTime.of(2026, 3, 26, 10, 5) : null
        );
    }
}
