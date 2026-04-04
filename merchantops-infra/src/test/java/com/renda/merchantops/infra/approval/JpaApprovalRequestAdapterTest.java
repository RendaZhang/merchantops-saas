package com.renda.merchantops.infra.approval;

import com.renda.merchantops.domain.approval.ApprovalRequestPageCriteria;
import com.renda.merchantops.domain.approval.ApprovalRequestRecord;
import com.renda.merchantops.domain.approval.ApprovalPendingRequestKeyPolicy;
import com.renda.merchantops.domain.shared.error.BizException;
import com.renda.merchantops.domain.shared.error.ErrorCode;
import com.renda.merchantops.infra.persistence.entity.ApprovalRequestEntity;
import com.renda.merchantops.infra.repository.ApprovalRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaApprovalRequestAdapterTest {

    @Mock
    private ApprovalRequestRepository approvalRequestRepository;

    @Test
    void saveShouldPersistDomainApprovalRequestFields() {
        JpaApprovalRequestAdapter adapter = new JpaApprovalRequestAdapter(approvalRequestRepository);
        when(approvalRequestRepository.save(any(ApprovalRequestEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ApprovalRequestRecord saved = adapter.save(new ApprovalRequestRecord(
                901L,
                1L,
                "USER_STATUS_DISABLE",
                "USER",
                103L,
                101L,
                105L,
                "APPROVED",
                "{\"status\":\"DISABLED\"}",
                null,
                "approve-req-1",
                LocalDateTime.of(2026, 3, 26, 10, 0),
                LocalDateTime.of(2026, 3, 26, 10, 5),
                LocalDateTime.of(2026, 3, 26, 10, 5)
        ));

        ArgumentCaptor<ApprovalRequestEntity> entityCaptor = ArgumentCaptor.forClass(ApprovalRequestEntity.class);
        verify(approvalRequestRepository).save(entityCaptor.capture());
        assertThat(entityCaptor.getValue().getStatus()).isEqualTo("APPROVED");
        assertThat(entityCaptor.getValue().getPendingRequestKey()).isNull();
        assertThat(saved.reviewedBy()).isEqualTo(105L);
    }

    @Test
    void saveShouldPersistPendingDisableKeyAndTranslateDuplicateConstraint() {
        JpaApprovalRequestAdapter adapter = new JpaApprovalRequestAdapter(approvalRequestRepository);
        when(approvalRequestRepository.save(any(ApprovalRequestEntity.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> adapter.save(new ApprovalRequestRecord(
                null,
                1L,
                "USER_STATUS_DISABLE",
                "USER",
                103L,
                101L,
                null,
                "PENDING",
                "{\"status\":\"DISABLED\"}",
                ApprovalPendingRequestKeyPolicy.userStatusDisableKey(1L, 103L),
                "disable-req-1",
                LocalDateTime.of(2026, 3, 26, 10, 0),
                null,
                null
        )))
                .isInstanceOf(BizException.class)
                .hasMessage("pending disable request already exists for user")
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BAD_REQUEST);

        ArgumentCaptor<ApprovalRequestEntity> entityCaptor = ArgumentCaptor.forClass(ApprovalRequestEntity.class);
        verify(approvalRequestRepository).save(entityCaptor.capture());
        assertThat(entityCaptor.getValue().getPendingRequestKey()).isEqualTo("USER_STATUS_DISABLE:1:103");
    }

    @Test
    void saveShouldTranslateDuplicateConstraintForImportReplayProposal() {
        JpaApprovalRequestAdapter adapter = new JpaApprovalRequestAdapter(approvalRequestRepository);
        when(approvalRequestRepository.save(any(ApprovalRequestEntity.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> adapter.save(new ApprovalRequestRecord(
                null,
                1L,
                "IMPORT_JOB_SELECTIVE_REPLAY",
                "IMPORT_JOB",
                7001L,
                101L,
                null,
                "PENDING",
                "{\"sourceJobId\":7001,\"errorCodes\":[\"UNKNOWN_ROLE\"]}",
                ApprovalPendingRequestKeyPolicy.importJobSelectiveReplayKey(1L, 7001L, List.of("UNKNOWN_ROLE")),
                "proposal-req-1",
                LocalDateTime.of(2026, 3, 29, 10, 0),
                null,
                null
        )))
                .isInstanceOf(BizException.class)
                .hasMessage("pending selective replay proposal already exists for source job and selected errorCodes");
    }

    @Test
    void saveShouldTranslateDuplicateConstraintForTicketCommentProposal() {
        JpaApprovalRequestAdapter adapter = new JpaApprovalRequestAdapter(approvalRequestRepository);
        when(approvalRequestRepository.save(any(ApprovalRequestEntity.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> adapter.save(new ApprovalRequestRecord(
                null,
                1L,
                "TICKET_COMMENT_CREATE",
                "TICKET",
                301L,
                101L,
                null,
                "PENDING",
                "{\"commentContent\":\"Reply draft content\"}",
                ApprovalPendingRequestKeyPolicy.ticketCommentCreateKey(1L, 301L, "Reply draft content"),
                "ticket-comment-proposal-1",
                LocalDateTime.of(2026, 3, 30, 10, 0),
                null,
                null
        )))
                .isInstanceOf(BizException.class)
                .hasMessage("pending ticket comment proposal already exists for ticket and comment content");
    }

    @Test
    void pageShouldMapRepositoryPageAndStableSort() {
        JpaApprovalRequestAdapter adapter = new JpaApprovalRequestAdapter(approvalRequestRepository);
        ApprovalRequestEntity entity = new ApprovalRequestEntity();
        entity.setId(901L);
        entity.setTenantId(1L);
        entity.setActionType("USER_STATUS_DISABLE");
        entity.setEntityType("USER");
        entity.setEntityId(103L);
        entity.setRequestedBy(101L);
        entity.setStatus("PENDING");
        entity.setPayloadJson("{\"status\":\"DISABLED\"}");
        entity.setRequestId("disable-req-1");
        entity.setCreatedAt(LocalDateTime.of(2026, 3, 26, 10, 0));
        when(approvalRequestRepository.searchPageByTenantId(
                eq(1L), eq("PENDING"), eq("USER_STATUS_DISABLE"), eq(101L), eq(Set.of("USER_STATUS_DISABLE", "TICKET_COMMENT_CREATE")), any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(entity), PageRequest.of(0, 10), 1));

        var result = adapter.page(1L, new ApprovalRequestPageCriteria(0, 10, "PENDING", "USER_STATUS_DISABLE", 101L, Set.of("USER_STATUS_DISABLE", "TICKET_COMMENT_CREATE")));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(approvalRequestRepository).searchPageByTenantId(eq(1L), eq("PENDING"), eq("USER_STATUS_DISABLE"), eq(101L), eq(Set.of("USER_STATUS_DISABLE", "TICKET_COMMENT_CREATE")), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("createdAt")).isNotNull();
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("id")).isNotNull();
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().getFirst().entityId()).isEqualTo(103L);
    }

    @Test
    void pageShouldReturnEmptyResultWhenNoActionTypesAreAllowed() {
        JpaApprovalRequestAdapter adapter = new JpaApprovalRequestAdapter(approvalRequestRepository);

        var result = adapter.page(1L, new ApprovalRequestPageCriteria(0, 10, "PENDING", null, null, Set.of()));

        assertThat(result.items()).isEmpty();
        assertThat(result.total()).isZero();
    }
}
