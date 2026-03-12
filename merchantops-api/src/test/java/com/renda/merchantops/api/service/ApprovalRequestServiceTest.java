package com.renda.merchantops.api.service;

import com.renda.merchantops.api.dto.approval.query.ApprovalRequestResponse;
import com.renda.merchantops.common.exception.BizException;
import com.renda.merchantops.common.exception.ErrorCode;
import com.renda.merchantops.infra.persistence.entity.ApprovalRequestEntity;
import com.renda.merchantops.infra.persistence.entity.UserEntity;
import com.renda.merchantops.infra.repository.ApprovalRequestRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApprovalRequestServiceTest {

    @Mock
    private ApprovalRequestRepository approvalRequestRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserCommandService userCommandService;

    @Mock
    private AuditEventService auditEventService;

    @InjectMocks
    private ApprovalRequestService approvalRequestService;

    @Test
    void createDisableRequestShouldLockTargetUserBeforeSavingRequest() {
        when(userRepository.findByIdAndTenantIdForUpdate(103L, 1L)).thenReturn(Optional.of(activeUser(103L, 1L, "viewer")));
        when(approvalRequestRepository.existsByTenantIdAndActionTypeAndEntityTypeAndEntityIdAndStatus(
                1L, "USER_STATUS_DISABLE", "USER", 103L, "PENDING"
        )).thenReturn(false);
        when(approvalRequestRepository.save(any(ApprovalRequestEntity.class))).thenAnswer(invocation -> {
            ApprovalRequestEntity entity = invocation.getArgument(0);
            entity.setId(901L);
            return entity;
        });

        ApprovalRequestResponse response = approvalRequestService.createDisableRequest(1L, 101L, "disable-req-1", 103L);

        assertThat(response.id()).isEqualTo(901L);
        assertThat(response.tenantId()).isEqualTo(1L);
        assertThat(response.entityId()).isEqualTo(103L);
        assertThat(response.status()).isEqualTo("PENDING");
        verify(userRepository).findByIdAndTenantIdForUpdate(103L, 1L);

        ArgumentCaptor<ApprovalRequestEntity> entityCaptor = ArgumentCaptor.forClass(ApprovalRequestEntity.class);
        verify(approvalRequestRepository).save(entityCaptor.capture());
        assertThat(entityCaptor.getValue().getActionType()).isEqualTo("USER_STATUS_DISABLE");
        assertThat(entityCaptor.getValue().getRequestedBy()).isEqualTo(101L);
        assertThat(entityCaptor.getValue().getPayloadJson()).isEqualTo("{\"status\":\"DISABLED\"}");
    }

    @Test
    void createDisableRequestShouldRejectDuplicatePendingRequest() {
        when(userRepository.findByIdAndTenantIdForUpdate(103L, 1L)).thenReturn(Optional.of(activeUser(103L, 1L, "viewer")));
        when(approvalRequestRepository.existsByTenantIdAndActionTypeAndEntityTypeAndEntityIdAndStatus(
                1L, "USER_STATUS_DISABLE", "USER", 103L, "PENDING"
        )).thenReturn(true);

        assertBizException(
                () -> approvalRequestService.createDisableRequest(1L, 101L, "disable-req-1", 103L),
                ErrorCode.BAD_REQUEST,
                "pending disable request already exists for user"
        );

        verify(userRepository).findByIdAndTenantIdForUpdate(103L, 1L);
    }

    @Test
    void approveShouldLockPendingRequestBeforeExecutingDisable() {
        ApprovalRequestEntity pending = pendingRequest(901L, 1L, 103L, 101L);
        when(approvalRequestRepository.findByIdAndTenantIdForUpdate(901L, 1L)).thenReturn(Optional.of(pending));
        when(userRepository.findByIdAndTenantIdForUpdate(103L, 1L)).thenReturn(Optional.of(activeUser(103L, 1L, "viewer")));
        when(approvalRequestRepository.save(any(ApprovalRequestEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ApprovalRequestResponse response = approvalRequestService.approve(1L, 105L, "approve-req-1", 901L);

        assertThat(response.status()).isEqualTo("APPROVED");
        assertThat(response.reviewedBy()).isEqualTo(105L);
        assertThat(response.reviewedAt()).isNotNull();
        assertThat(response.executedAt()).isNotNull();
        verify(approvalRequestRepository).findByIdAndTenantIdForUpdate(901L, 1L);
        verify(userRepository).findByIdAndTenantIdForUpdate(103L, 1L);
        verify(userCommandService).updateStatus(eq(1L), eq(105L), eq("approve-req-1"), eq(103L), any());
    }

    @Test
    void rejectShouldLockPendingRequestBeforeUpdatingStatus() {
        ApprovalRequestEntity pending = pendingRequest(901L, 1L, 103L, 101L);
        when(approvalRequestRepository.findByIdAndTenantIdForUpdate(901L, 1L)).thenReturn(Optional.of(pending));
        when(approvalRequestRepository.save(any(ApprovalRequestEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ApprovalRequestResponse response = approvalRequestService.reject(1L, 105L, "reject-req-1", 901L);

        assertThat(response.status()).isEqualTo("REJECTED");
        assertThat(response.reviewedBy()).isEqualTo(105L);
        assertThat(response.reviewedAt()).isNotNull();
        verify(approvalRequestRepository).findByIdAndTenantIdForUpdate(901L, 1L);
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

    private UserEntity activeUser(Long id, Long tenantId, String username) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setTenantId(tenantId);
        user.setUsername(username);
        user.setStatus("ACTIVE");
        return user;
    }

    private ApprovalRequestEntity pendingRequest(Long id, Long tenantId, Long entityId, Long requestedBy) {
        ApprovalRequestEntity entity = new ApprovalRequestEntity();
        entity.setId(id);
        entity.setTenantId(tenantId);
        entity.setActionType("USER_STATUS_DISABLE");
        entity.setEntityType("USER");
        entity.setEntityId(entityId);
        entity.setRequestedBy(requestedBy);
        entity.setStatus("PENDING");
        entity.setPayloadJson("{\"status\":\"DISABLED\"}");
        entity.setRequestId("disable-req-create-1");
        entity.setCreatedAt(LocalDateTime.now());
        return entity;
    }
}
