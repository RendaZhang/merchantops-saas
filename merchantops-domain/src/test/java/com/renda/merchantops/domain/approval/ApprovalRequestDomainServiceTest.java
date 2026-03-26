package com.renda.merchantops.domain.approval;

import com.renda.merchantops.domain.shared.error.BizException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApprovalRequestDomainServiceTest {

    @Test
    void createDisableRequestShouldLockActiveUserBeforeSavingPendingRequest() {
        CapturingApprovalRequestPort requestPort = new CapturingApprovalRequestPort();
        requestPort.existsPendingDisableRequest = false;
        requestPort.savedResponse = approvalRequest(901L, "PENDING", null, null);
        CapturingApprovalTargetUserPort userPort = new CapturingApprovalTargetUserPort(Optional.of(new ApprovalTargetUser(103L, "ACTIVE")));
        ApprovalRequestUseCase useCase = new ApprovalRequestDomainService(requestPort, userPort, new NoopApprovalActionPort());

        ApprovalRequestRecord result = useCase.createDisableRequest(1L, 101L, "disable-req-1", 103L);

        assertThat(result.id()).isEqualTo(901L);
        assertThat(requestPort.savedRequest.actionType()).isEqualTo("USER_STATUS_DISABLE");
        assertThat(requestPort.savedRequest.entityType()).isEqualTo("USER");
        assertThat(requestPort.savedRequest.entityId()).isEqualTo(103L);
        assertThat(requestPort.savedRequest.status()).isEqualTo("PENDING");
        assertThat(requestPort.savedRequest.payloadJson()).isEqualTo("{\"status\":\"DISABLED\"}");
    }

    @Test
    void approveShouldDisableUserAndPersistApprovedStatus() {
        CapturingApprovalRequestPort requestPort = new CapturingApprovalRequestPort();
        requestPort.lockedRequest = Optional.of(approvalRequest(901L, "PENDING", 103L, 101L));
        requestPort.savedResponse = approvalRequest(901L, "APPROVED", 103L, 101L);
        CapturingApprovalTargetUserPort userPort = new CapturingApprovalTargetUserPort(Optional.of(new ApprovalTargetUser(103L, "ACTIVE")));
        CapturingApprovalActionPort actionPort = new CapturingApprovalActionPort();
        ApprovalRequestUseCase useCase = new ApprovalRequestDomainService(requestPort, userPort, actionPort);

        ApprovalRequestRecord result = useCase.approve(1L, 105L, "approve-req-1", 901L);

        assertThat(actionPort.tenantId).isEqualTo(1L);
        assertThat(actionPort.reviewerId).isEqualTo(105L);
        assertThat(actionPort.requestId).isEqualTo("approve-req-1");
        assertThat(actionPort.userId).isEqualTo(103L);
        assertThat(requestPort.savedRequest.status()).isEqualTo("APPROVED");
        assertThat(requestPort.savedRequest.reviewedBy()).isEqualTo(105L);
        assertThat(requestPort.savedRequest.reviewedAt()).isNotNull();
        assertThat(requestPort.savedRequest.executedAt()).isNotNull();
        assertThat(result.status()).isEqualTo("APPROVED");
    }

    @Test
    void rejectShouldPreventSelfReview() {
        CapturingApprovalRequestPort requestPort = new CapturingApprovalRequestPort();
        requestPort.lockedRequest = Optional.of(approvalRequest(901L, "PENDING", 103L, 101L));
        ApprovalRequestUseCase useCase = new ApprovalRequestDomainService(
                requestPort,
                new CapturingApprovalTargetUserPort(Optional.of(new ApprovalTargetUser(103L, "ACTIVE"))),
                new NoopApprovalActionPort()
        );

        assertThatThrownBy(() -> useCase.reject(1L, 101L, "reject-req-1", 901L))
                .isInstanceOf(BizException.class)
                .hasMessage("requester cannot approve or reject own request");
    }

    @Test
    void pageShouldNormalizeFiltersAndBounds() {
        CapturingApprovalRequestPort requestPort = new CapturingApprovalRequestPort();
        requestPort.pageResult = new ApprovalRequestPageResult(List.of(), 0, 10, 0, 0);
        ApprovalRequestUseCase useCase = new ApprovalRequestDomainService(
                requestPort,
                new CapturingApprovalTargetUserPort(Optional.of(new ApprovalTargetUser(103L, "ACTIVE"))),
                new NoopApprovalActionPort()
        );

        useCase.page(1L, new ApprovalRequestPageCriteria(-1, 0, "  PENDING  ", " USER_STATUS_DISABLE ", 101L));

        assertThat(requestPort.pageCriteria.page()).isEqualTo(0);
        assertThat(requestPort.pageCriteria.size()).isEqualTo(10);
        assertThat(requestPort.pageCriteria.status()).isEqualTo("PENDING");
        assertThat(requestPort.pageCriteria.actionType()).isEqualTo("USER_STATUS_DISABLE");
        assertThat(requestPort.pageCriteria.requestedBy()).isEqualTo(101L);
    }

    private ApprovalRequestRecord approvalRequest(Long id, String status, Long entityId, Long requestedBy) {
        return new ApprovalRequestRecord(
                id,
                1L,
                "USER_STATUS_DISABLE",
                "USER",
                entityId,
                requestedBy,
                null,
                status,
                "{\"status\":\"DISABLED\"}",
                "disable-req-1",
                LocalDateTime.of(2026, 3, 26, 10, 0),
                null,
                null
        );
    }

    private static final class CapturingApprovalRequestPort implements ApprovalRequestPort {

        private boolean existsPendingDisableRequest;
        private ApprovalRequestRecord savedRequest;
        private ApprovalRequestRecord savedResponse;
        private Optional<ApprovalRequestRecord> lockedRequest = Optional.empty();
        private ApprovalRequestPageCriteria pageCriteria;
        private ApprovalRequestPageResult pageResult;

        @Override
        public boolean existsPendingDisableRequest(Long tenantId, Long userId) {
            return existsPendingDisableRequest;
        }

        @Override
        public ApprovalRequestRecord save(ApprovalRequestRecord request) {
            this.savedRequest = request;
            return savedResponse == null ? request : savedResponse;
        }

        @Override
        public Optional<ApprovalRequestRecord> findById(Long tenantId, Long approvalRequestId) {
            return lockedRequest;
        }

        @Override
        public Optional<ApprovalRequestRecord> findByIdForUpdate(Long tenantId, Long approvalRequestId) {
            return lockedRequest;
        }

        @Override
        public ApprovalRequestPageResult page(Long tenantId, ApprovalRequestPageCriteria criteria) {
            this.pageCriteria = criteria;
            return pageResult;
        }
    }

    private record CapturingApprovalTargetUserPort(Optional<ApprovalTargetUser> user) implements ApprovalTargetUserPort {

        @Override
        public Optional<ApprovalTargetUser> findForDisable(Long tenantId, Long userId) {
            return user;
        }
    }

    private static final class CapturingApprovalActionPort implements ApprovalActionPort {

        private Long tenantId;
        private Long reviewerId;
        private String requestId;
        private Long userId;

        @Override
        public void disableUser(Long tenantId, Long reviewerId, String requestId, Long userId) {
            this.tenantId = tenantId;
            this.reviewerId = reviewerId;
            this.requestId = requestId;
            this.userId = userId;
        }
    }

    private static final class NoopApprovalActionPort implements ApprovalActionPort {

        @Override
        public void disableUser(Long tenantId, Long reviewerId, String requestId, Long userId) {
        }
    }
}
