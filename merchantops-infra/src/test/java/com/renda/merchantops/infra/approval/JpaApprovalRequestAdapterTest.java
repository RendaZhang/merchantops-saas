package com.renda.merchantops.infra.approval;

import com.renda.merchantops.domain.approval.ApprovalRequestPageCriteria;
import com.renda.merchantops.domain.approval.ApprovalRequestRecord;
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
    void saveShouldDerivePendingDisableKeyAndTranslateDuplicateConstraint() {
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
                "disable-req-1",
                LocalDateTime.of(2026, 3, 26, 10, 0),
                null,
                null
        )))
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BAD_REQUEST);

        ArgumentCaptor<ApprovalRequestEntity> entityCaptor = ArgumentCaptor.forClass(ApprovalRequestEntity.class);
        verify(approvalRequestRepository).save(entityCaptor.capture());
        assertThat(entityCaptor.getValue().getPendingRequestKey()).isEqualTo("USER_STATUS_DISABLE:1:103");
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
                eq(1L), eq("PENDING"), eq("USER_STATUS_DISABLE"), eq(101L), any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(entity), PageRequest.of(0, 10), 1));

        var result = adapter.page(1L, new ApprovalRequestPageCriteria(0, 10, "PENDING", "USER_STATUS_DISABLE", 101L));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(approvalRequestRepository).searchPageByTenantId(eq(1L), eq("PENDING"), eq("USER_STATUS_DISABLE"), eq(101L), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("createdAt")).isNotNull();
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("id")).isNotNull();
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().getFirst().entityId()).isEqualTo(103L);
    }
}
