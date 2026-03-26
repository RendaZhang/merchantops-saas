package com.renda.merchantops.api.approval;

import com.renda.merchantops.api.audit.AuditEventService;
import com.renda.merchantops.api.dto.approval.query.ApprovalRequestPageQuery;
import com.renda.merchantops.domain.approval.ApprovalRequestPageCriteria;
import com.renda.merchantops.domain.approval.ApprovalRequestPageResult;
import com.renda.merchantops.domain.approval.ApprovalRequestRecord;
import com.renda.merchantops.domain.approval.ApprovalRequestUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

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
                .thenReturn(record(901L, "PENDING", 103L, 101L));

        var response = approvalRequestCommandService.createDisableRequest(1L, 101L, "disable-req-1", 103L);

        assertThat(response.id()).isEqualTo(901L);
        assertThat(response.status()).isEqualTo("PENDING");
        verify(approvalRequestUseCase).createDisableRequest(1L, 101L, "disable-req-1", 103L);
        verify(auditEventService).recordEvent(eq(1L), eq("APPROVAL_REQUEST"), eq(901L), eq("APPROVAL_REQUEST_CREATED"), eq(101L), eq("disable-req-1"), eq(null), any());
    }

    @Test
    void getByIdShouldMapDomainRecordToResponse() {
        when(approvalRequestUseCase.getById(1L, 901L)).thenReturn(record(901L, "PENDING", 103L, 101L));

        var response = approvalRequestQueryService.getById(1L, 901L);

        assertThat(response.id()).isEqualTo(901L);
        assertThat(response.entityId()).isEqualTo(103L);
        assertThat(response.requestedBy()).isEqualTo(101L);
    }

    @Test
    void pageShouldMapCriteriaAndDomainPage() {
        when(approvalRequestUseCase.page(eq(1L), any()))
                .thenReturn(new ApprovalRequestPageResult(List.of(record(901L, "PENDING", 103L, 101L)), 0, 10, 1, 1));

        var response = approvalRequestQueryService.page(1L, new ApprovalRequestPageQuery(-1, 0, "  PENDING  ", " USER_STATUS_DISABLE ", 101L));

        ArgumentCaptor<ApprovalRequestPageCriteria> criteriaCaptor = ArgumentCaptor.forClass(ApprovalRequestPageCriteria.class);
        verify(approvalRequestUseCase).page(eq(1L), criteriaCaptor.capture());
        assertThat(criteriaCaptor.getValue().page()).isEqualTo(-1);
        assertThat(criteriaCaptor.getValue().size()).isEqualTo(0);
        assertThat(criteriaCaptor.getValue().status()).isEqualTo("  PENDING  ");
        assertThat(criteriaCaptor.getValue().actionType()).isEqualTo(" USER_STATUS_DISABLE ");
        assertThat(criteriaCaptor.getValue().requestedBy()).isEqualTo(101L);
        assertThat(response.getTotal()).isEqualTo(1);
        assertThat(response.getItems()).hasSize(1);
    }

    @Test
    void approveShouldDelegateAndEmitApprovalAndExecutionAuditEvents() {
        when(approvalRequestUseCase.approve(1L, 105L, "approve-req-1", 901L))
                .thenReturn(record(901L, "APPROVED", 103L, 101L));

        var response = approvalRequestCommandService.approve(1L, 105L, "approve-req-1", 901L);

        assertThat(response.status()).isEqualTo("APPROVED");
        verify(approvalRequestUseCase).approve(1L, 105L, "approve-req-1", 901L);
        verify(auditEventService, times(2)).recordEvent(eq(1L), eq("APPROVAL_REQUEST"), eq(901L), any(), eq(105L), eq("approve-req-1"), eq(null), any());
    }

    @Test
    void rejectShouldDelegateAndEmitRejectedAuditEvent() {
        when(approvalRequestUseCase.reject(1L, 105L, "reject-req-1", 901L))
                .thenReturn(record(901L, "REJECTED", 103L, 101L));

        var response = approvalRequestCommandService.reject(1L, 105L, "reject-req-1", 901L);

        assertThat(response.status()).isEqualTo("REJECTED");
        verify(approvalRequestUseCase).reject(1L, 105L, "reject-req-1", 901L);
        verify(auditEventService).recordEvent(eq(1L), eq("APPROVAL_REQUEST"), eq(901L), eq("APPROVAL_REQUEST_REJECTED"), eq(105L), eq("reject-req-1"), eq(null), any());
    }

    private ApprovalRequestRecord record(Long id, String status, Long entityId, Long requestedBy) {
        return new ApprovalRequestRecord(
                id,
                1L,
                "USER_STATUS_DISABLE",
                "USER",
                entityId,
                requestedBy,
                "APPROVED".equals(status) ? 105L : null,
                status,
                "{\"status\":\"DISABLED\"}",
                "disable-req-1",
                LocalDateTime.of(2026, 3, 26, 10, 0),
                "PENDING".equals(status) ? null : LocalDateTime.of(2026, 3, 26, 10, 5),
                "APPROVED".equals(status) ? LocalDateTime.of(2026, 3, 26, 10, 5) : null
        );
    }
}
