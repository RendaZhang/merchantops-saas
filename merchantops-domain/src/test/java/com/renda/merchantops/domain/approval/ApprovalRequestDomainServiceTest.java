package com.renda.merchantops.domain.approval;

import com.renda.merchantops.domain.shared.error.BizException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApprovalRequestDomainServiceTest {

    @Test
    void createDisableRequestShouldLockActiveUserBeforeSavingPendingRequest() {
        CapturingApprovalRequestPort requestPort = new CapturingApprovalRequestPort();
        requestPort.savedResponse = approvalRequest(901L, "PENDING", null, null);
        CapturingApprovalTargetUserPort userPort = new CapturingApprovalTargetUserPort(Optional.of(new ApprovalTargetUser(103L, "ACTIVE")));
        ApprovalRequestUseCase useCase = new ApprovalRequestDomainService(
                requestPort,
                userPort,
                new NoopApprovalActionPort(),
                new NoopApprovalImportSelectiveReplayPort(),
                new NoopApprovalTicketCommentProposalPort()
        );

        ApprovalRequestRecord result = useCase.createDisableRequest(1L, 101L, "disable-req-1", 103L);

        assertThat(result.id()).isEqualTo(901L);
        assertThat(requestPort.savedRequest.actionType()).isEqualTo("USER_STATUS_DISABLE");
        assertThat(requestPort.savedRequest.entityType()).isEqualTo("USER");
        assertThat(requestPort.savedRequest.entityId()).isEqualTo(103L);
        assertThat(requestPort.savedRequest.status()).isEqualTo("PENDING");
        assertThat(requestPort.savedRequest.payloadJson()).isEqualTo("{\"status\":\"DISABLED\"}");
        assertThat(requestPort.savedRequest.pendingRequestKey()).isEqualTo("USER_STATUS_DISABLE:1:103");
    }

    @Test
    void createImportSelectiveReplayRequestShouldPersistPreparedApprovalPayload() {
        CapturingApprovalRequestPort requestPort = new CapturingApprovalRequestPort();
        requestPort.savedResponse = importApprovalRequest(902L, "PENDING", 7001L, 101L, "{\"sourceJobId\":7001,\"errorCodes\":[\"UNKNOWN_ROLE\"]}");
        CapturingApprovalImportSelectiveReplayPort importReplayPort = new CapturingApprovalImportSelectiveReplayPort();
        importReplayPort.prepared = new PreparedImportSelectiveReplayApproval(
                7001L,
                List.of("UNKNOWN_ROLE"),
                9103L,
                "Review role fixes before replay",
                "{\"sourceJobId\":7001,\"errorCodes\":[\"UNKNOWN_ROLE\"],\"sourceInteractionId\":9103,\"proposalReason\":\"Review role fixes before replay\"}",
                ApprovalPendingRequestKeyPolicy.importJobSelectiveReplayKey(1L, 7001L, List.of("UNKNOWN_ROLE"))
        );
        ApprovalRequestUseCase useCase = new ApprovalRequestDomainService(
                requestPort,
                new CapturingApprovalTargetUserPort(Optional.of(new ApprovalTargetUser(103L, "ACTIVE"))),
                new NoopApprovalActionPort(),
                importReplayPort,
                new NoopApprovalTicketCommentProposalPort()
        );

        ApprovalRequestRecord result = useCase.createImportSelectiveReplayRequest(
                1L,
                101L,
                "proposal-req-1",
                new ImportSelectiveReplayApprovalCommand(7001L, List.of("UNKNOWN_ROLE"), 9103L, "Review role fixes before replay")
        );

        assertThat(result.id()).isEqualTo(902L);
        assertThat(importReplayPort.command).isEqualTo(
                new ImportSelectiveReplayApprovalCommand(7001L, List.of("UNKNOWN_ROLE"), 9103L, "Review role fixes before replay")
        );
        assertThat(requestPort.savedRequest.actionType()).isEqualTo("IMPORT_JOB_SELECTIVE_REPLAY");
        assertThat(requestPort.savedRequest.entityType()).isEqualTo("IMPORT_JOB");
        assertThat(requestPort.savedRequest.entityId()).isEqualTo(7001L);
        assertThat(requestPort.savedRequest.payloadJson()).isEqualTo(
                "{\"sourceJobId\":7001,\"errorCodes\":[\"UNKNOWN_ROLE\"],\"sourceInteractionId\":9103,\"proposalReason\":\"Review role fixes before replay\"}"
        );
        assertThat(requestPort.savedRequest.pendingRequestKey()).isEqualTo(
                ApprovalPendingRequestKeyPolicy.importJobSelectiveReplayKey(1L, 7001L, List.of("UNKNOWN_ROLE"))
        );
    }

    @Test
    void approveShouldDisableUserAndPersistApprovedStatus() {
        CapturingApprovalRequestPort requestPort = new CapturingApprovalRequestPort();
        requestPort.lockedRequest = Optional.of(approvalRequest(901L, "PENDING", 103L, 101L));
        requestPort.savedResponse = approvalRequest(901L, "APPROVED", 103L, 101L);
        CapturingApprovalTargetUserPort userPort = new CapturingApprovalTargetUserPort(Optional.of(new ApprovalTargetUser(103L, "ACTIVE")));
        CapturingApprovalActionPort actionPort = new CapturingApprovalActionPort();
        ApprovalRequestUseCase useCase = new ApprovalRequestDomainService(
                requestPort,
                userPort,
                actionPort,
                new NoopApprovalImportSelectiveReplayPort(),
                new NoopApprovalTicketCommentProposalPort()
        );

        ApprovalRequestRecord result = useCase.approve(1L, 105L, "approve-req-1", 901L);

        assertThat(actionPort.tenantId).isEqualTo(1L);
        assertThat(actionPort.reviewerId).isEqualTo(105L);
        assertThat(actionPort.requestId).isEqualTo("approve-req-1");
        assertThat(actionPort.userId).isEqualTo(103L);
        assertThat(requestPort.savedRequest.status()).isEqualTo("APPROVED");
        assertThat(requestPort.savedRequest.reviewedBy()).isEqualTo(105L);
        assertThat(requestPort.savedRequest.reviewedAt()).isNotNull();
        assertThat(requestPort.savedRequest.executedAt()).isNotNull();
        assertThat(requestPort.savedRequest.pendingRequestKey()).isNull();
        assertThat(result.status()).isEqualTo("APPROVED");
    }

    @Test
    void approveImportSelectiveReplayShouldDispatchReplayActionAndPersistApprovedStatus() {
        CapturingApprovalRequestPort requestPort = new CapturingApprovalRequestPort();
        requestPort.lockedRequest = Optional.of(importApprovalRequest(
                902L,
                "PENDING",
                7001L,
                101L,
                "{\"sourceJobId\":7001,\"errorCodes\":[\"UNKNOWN_ROLE\"]}"
        ));
        requestPort.savedResponse = importApprovalRequest(
                902L,
                "APPROVED",
                7001L,
                101L,
                "{\"sourceJobId\":7001,\"errorCodes\":[\"UNKNOWN_ROLE\"]}"
        );
        CapturingApprovalActionPort actionPort = new CapturingApprovalActionPort();
        ApprovalRequestUseCase useCase = new ApprovalRequestDomainService(
                requestPort,
                new CapturingApprovalTargetUserPort(Optional.of(new ApprovalTargetUser(103L, "ACTIVE"))),
                actionPort,
                new NoopApprovalImportSelectiveReplayPort(),
                new NoopApprovalTicketCommentProposalPort()
        );

        ApprovalRequestRecord result = useCase.approve(1L, 105L, "approve-import-proposal-1", 902L);

        assertThat(actionPort.sourceJobId).isEqualTo(7001L);
        assertThat(actionPort.payloadJson).isEqualTo("{\"sourceJobId\":7001,\"errorCodes\":[\"UNKNOWN_ROLE\"]}");
        assertThat(result.status()).isEqualTo("APPROVED");
        assertThat(requestPort.savedRequest.executedAt()).isNotNull();
        assertThat(requestPort.savedRequest.pendingRequestKey()).isNull();
    }

    @Test
    void rejectShouldPreventSelfReview() {
        CapturingApprovalRequestPort requestPort = new CapturingApprovalRequestPort();
        requestPort.lockedRequest = Optional.of(approvalRequest(901L, "PENDING", 103L, 101L));
        ApprovalRequestUseCase useCase = new ApprovalRequestDomainService(
                requestPort,
                new CapturingApprovalTargetUserPort(Optional.of(new ApprovalTargetUser(103L, "ACTIVE"))),
                new NoopApprovalActionPort(),
                new NoopApprovalImportSelectiveReplayPort(),
                new NoopApprovalTicketCommentProposalPort()
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
                new NoopApprovalActionPort(),
                new NoopApprovalImportSelectiveReplayPort(),
                new NoopApprovalTicketCommentProposalPort()
        );

        useCase.page(1L, new ApprovalRequestPageCriteria(-1, 0, "  PENDING  ", " USER_STATUS_DISABLE ", 101L, Set.of(" ticket_comment_create ", "USER_STATUS_DISABLE")));

        assertThat(requestPort.pageCriteria.page()).isEqualTo(0);
        assertThat(requestPort.pageCriteria.size()).isEqualTo(10);
        assertThat(requestPort.pageCriteria.status()).isEqualTo("PENDING");
        assertThat(requestPort.pageCriteria.actionType()).isEqualTo("USER_STATUS_DISABLE");
        assertThat(requestPort.pageCriteria.requestedBy()).isEqualTo(101L);
        assertThat(requestPort.pageCriteria.allowedActionTypes()).containsExactlyInAnyOrder("TICKET_COMMENT_CREATE", "USER_STATUS_DISABLE");
    }

    @Test
    void createTicketCommentRequestShouldPersistPreparedApprovalPayload() {
        CapturingApprovalRequestPort requestPort = new CapturingApprovalRequestPort();
        requestPort.savedResponse = ticketCommentApprovalRequest(903L, "PENDING", 301L, 101L, "{\"commentContent\":\"Reply draft content\",\"sourceInteractionId\":9002}");
        CapturingApprovalTicketCommentProposalPort ticketCommentPort = new CapturingApprovalTicketCommentProposalPort();
        ticketCommentPort.prepared = new PreparedTicketCommentApproval(
                301L,
                "Reply draft content",
                9002L,
                "{\"commentContent\":\"Reply draft content\",\"sourceInteractionId\":9002}",
                ApprovalPendingRequestKeyPolicy.ticketCommentCreateKey(1L, 301L, "Reply draft content")
        );
        ApprovalRequestUseCase useCase = new ApprovalRequestDomainService(
                requestPort,
                new CapturingApprovalTargetUserPort(Optional.of(new ApprovalTargetUser(103L, "ACTIVE"))),
                new NoopApprovalActionPort(),
                new NoopApprovalImportSelectiveReplayPort(),
                ticketCommentPort
        );

        ApprovalRequestRecord result = useCase.createTicketCommentRequest(
                1L,
                101L,
                "ticket-comment-proposal-1",
                new TicketCommentApprovalCommand(301L, "Reply draft content", 9002L)
        );

        assertThat(result.id()).isEqualTo(903L);
        assertThat(ticketCommentPort.command).isEqualTo(new TicketCommentApprovalCommand(301L, "Reply draft content", 9002L));
        assertThat(requestPort.savedRequest.actionType()).isEqualTo("TICKET_COMMENT_CREATE");
        assertThat(requestPort.savedRequest.entityType()).isEqualTo("TICKET");
        assertThat(requestPort.savedRequest.entityId()).isEqualTo(301L);
        assertThat(requestPort.savedRequest.payloadJson()).isEqualTo("{\"commentContent\":\"Reply draft content\",\"sourceInteractionId\":9002}");
        assertThat(requestPort.savedRequest.pendingRequestKey()).isEqualTo(
                ApprovalPendingRequestKeyPolicy.ticketCommentCreateKey(1L, 301L, "Reply draft content")
        );
    }

    @Test
    void createTicketCommentRequestShouldRejectDuplicatePendingRequestBeforeSave() {
        CapturingApprovalRequestPort requestPort = new CapturingApprovalRequestPort();
        String pendingRequestKey = ApprovalPendingRequestKeyPolicy.ticketCommentCreateKey(1L, 301L, "Reply draft content");
        requestPort.existingPendingKeys = Set.of(pendingRequestKey);
        CapturingApprovalTicketCommentProposalPort ticketCommentPort = new CapturingApprovalTicketCommentProposalPort();
        ticketCommentPort.prepared = new PreparedTicketCommentApproval(
                301L,
                "Reply draft content",
                9002L,
                "{\"commentContent\":\"Reply draft content\",\"sourceInteractionId\":9002}",
                pendingRequestKey
        );
        ApprovalRequestUseCase useCase = new ApprovalRequestDomainService(
                requestPort,
                new CapturingApprovalTargetUserPort(Optional.of(new ApprovalTargetUser(103L, "ACTIVE"))),
                new NoopApprovalActionPort(),
                new NoopApprovalImportSelectiveReplayPort(),
                ticketCommentPort
        );

        assertThatThrownBy(() -> useCase.createTicketCommentRequest(
                1L,
                101L,
                "ticket-comment-proposal-duplicate-precheck",
                new TicketCommentApprovalCommand(301L, "Reply draft content", 9002L)
        ))
                .isInstanceOf(BizException.class)
                .hasMessage("pending ticket comment proposal already exists for ticket and comment content");
        assertThat(requestPort.savedRequest).isNull();
    }

    @Test
    void approveTicketCommentShouldDispatchCommentCreationAndPersistApprovedStatus() {
        CapturingApprovalRequestPort requestPort = new CapturingApprovalRequestPort();
        requestPort.lockedRequest = Optional.of(ticketCommentApprovalRequest(
                903L,
                "PENDING",
                301L,
                101L,
                "{\"commentContent\":\"Reply draft content\",\"sourceInteractionId\":9002}"
        ));
        requestPort.savedResponse = ticketCommentApprovalRequest(
                903L,
                "APPROVED",
                301L,
                101L,
                "{\"commentContent\":\"Reply draft content\",\"sourceInteractionId\":9002}"
        );
        CapturingApprovalActionPort actionPort = new CapturingApprovalActionPort();
        ApprovalRequestUseCase useCase = new ApprovalRequestDomainService(
                requestPort,
                new CapturingApprovalTargetUserPort(Optional.of(new ApprovalTargetUser(103L, "ACTIVE"))),
                actionPort,
                new NoopApprovalImportSelectiveReplayPort(),
                new NoopApprovalTicketCommentProposalPort()
        );

        ApprovalRequestRecord result = useCase.approve(1L, 105L, "approve-ticket-comment-proposal-1", 903L);

        assertThat(actionPort.ticketId).isEqualTo(301L);
        assertThat(actionPort.payloadJson).isEqualTo("{\"commentContent\":\"Reply draft content\",\"sourceInteractionId\":9002}");
        assertThat(result.status()).isEqualTo("APPROVED");
        assertThat(requestPort.savedRequest.executedAt()).isNotNull();
        assertThat(requestPort.savedRequest.pendingRequestKey()).isNull();
    }

    @Test
    void rejectShouldPersistRejectedStatusAndClearPendingRequestKey() {
        CapturingApprovalRequestPort requestPort = new CapturingApprovalRequestPort();
        requestPort.lockedRequest = Optional.of(ticketCommentApprovalRequest(
                903L,
                "PENDING",
                301L,
                101L,
                "{\"commentContent\":\"Reply draft content\",\"sourceInteractionId\":9002}"
        ));
        requestPort.savedResponse = ticketCommentApprovalRequest(
                903L,
                "REJECTED",
                301L,
                101L,
                "{\"commentContent\":\"Reply draft content\",\"sourceInteractionId\":9002}"
        );
        ApprovalRequestUseCase useCase = new ApprovalRequestDomainService(
                requestPort,
                new CapturingApprovalTargetUserPort(Optional.of(new ApprovalTargetUser(103L, "ACTIVE"))),
                new NoopApprovalActionPort(),
                new NoopApprovalImportSelectiveReplayPort(),
                new NoopApprovalTicketCommentProposalPort()
        );

        ApprovalRequestRecord result = useCase.reject(1L, 105L, "reject-ticket-comment-proposal-1", 903L);

        assertThat(result.status()).isEqualTo("REJECTED");
        assertThat(requestPort.savedRequest.status()).isEqualTo("REJECTED");
        assertThat(requestPort.savedRequest.reviewedBy()).isEqualTo(105L);
        assertThat(requestPort.savedRequest.reviewedAt()).isNotNull();
        assertThat(requestPort.savedRequest.executedAt()).isNull();
        assertThat(requestPort.savedRequest.pendingRequestKey()).isNull();
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
                "PENDING".equals(status) && entityId != null
                        ? ApprovalPendingRequestKeyPolicy.userStatusDisableKey(1L, entityId)
                        : null,
                "disable-req-1",
                LocalDateTime.of(2026, 3, 26, 10, 0),
                null,
                null
        );
    }

    private ApprovalRequestRecord importApprovalRequest(Long id,
                                                        String status,
                                                        Long entityId,
                                                        Long requestedBy,
                                                        String payloadJson) {
        return new ApprovalRequestRecord(
                id,
                1L,
                "IMPORT_JOB_SELECTIVE_REPLAY",
                "IMPORT_JOB",
                entityId,
                requestedBy,
                null,
                status,
                payloadJson,
                "PENDING".equals(status)
                        ? ApprovalPendingRequestKeyPolicy.importJobSelectiveReplayKey(1L, entityId, List.of("UNKNOWN_ROLE"))
                        : null,
                "proposal-req-1",
                LocalDateTime.of(2026, 3, 29, 10, 0),
                null,
                null
        );
    }

    private ApprovalRequestRecord ticketCommentApprovalRequest(Long id,
                                                               String status,
                                                               Long entityId,
                                                               Long requestedBy,
                                                               String payloadJson) {
        return new ApprovalRequestRecord(
                id,
                1L,
                "TICKET_COMMENT_CREATE",
                "TICKET",
                entityId,
                requestedBy,
                null,
                status,
                payloadJson,
                "PENDING".equals(status)
                        ? ApprovalPendingRequestKeyPolicy.ticketCommentCreateKey(1L, entityId, "Reply draft content")
                        : null,
                "ticket-comment-proposal-1",
                LocalDateTime.of(2026, 3, 30, 10, 0),
                null,
                null
        );
    }

    private static final class CapturingApprovalRequestPort implements ApprovalRequestPort {

        private ApprovalRequestRecord savedRequest;
        private ApprovalRequestRecord savedResponse;
        private Optional<ApprovalRequestRecord> lockedRequest = Optional.empty();
        private ApprovalRequestPageCriteria pageCriteria;
        private ApprovalRequestPageResult pageResult;
        private Set<String> existingPendingKeys = Set.of();

        @Override
        public ApprovalRequestRecord save(ApprovalRequestRecord request) {
            this.savedRequest = request;
            return savedResponse == null ? request : savedResponse;
        }

        @Override
        public boolean existsPendingRequestKey(Long tenantId, String pendingRequestKey) {
            return existingPendingKeys.contains(pendingRequestKey);
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
        private Long sourceJobId;
        private Long ticketId;
        private String payloadJson;

        @Override
        public void disableUser(Long tenantId, Long reviewerId, String requestId, Long userId) {
            this.tenantId = tenantId;
            this.reviewerId = reviewerId;
            this.requestId = requestId;
            this.userId = userId;
        }

        @Override
        public void replayImportJobSelective(Long tenantId,
                                             Long reviewerId,
                                             String requestId,
                                             Long sourceJobId,
                                             String payloadJson) {
            this.tenantId = tenantId;
            this.reviewerId = reviewerId;
            this.requestId = requestId;
            this.sourceJobId = sourceJobId;
            this.payloadJson = payloadJson;
        }

        @Override
        public void createTicketComment(Long tenantId,
                                        Long reviewerId,
                                        String requestId,
                                        Long ticketId,
                                        String payloadJson) {
            this.tenantId = tenantId;
            this.reviewerId = reviewerId;
            this.requestId = requestId;
            this.ticketId = ticketId;
            this.payloadJson = payloadJson;
        }
    }

    private static final class NoopApprovalActionPort implements ApprovalActionPort {

        @Override
        public void disableUser(Long tenantId, Long reviewerId, String requestId, Long userId) {
        }

        @Override
        public void replayImportJobSelective(Long tenantId,
                                             Long reviewerId,
                                             String requestId,
                                             Long sourceJobId,
                                             String payloadJson) {
        }

        @Override
        public void createTicketComment(Long tenantId,
                                        Long reviewerId,
                                        String requestId,
                                        Long ticketId,
                                        String payloadJson) {
        }
    }

    private static final class CapturingApprovalImportSelectiveReplayPort implements ApprovalImportSelectiveReplayPort {

        private ImportSelectiveReplayApprovalCommand command;
        private PreparedImportSelectiveReplayApproval prepared;

        @Override
        public PreparedImportSelectiveReplayApproval prepareProposal(Long tenantId, ImportSelectiveReplayApprovalCommand command) {
            this.command = command;
            return prepared;
        }
    }

    private static final class CapturingApprovalTicketCommentProposalPort implements ApprovalTicketCommentProposalPort {

        private TicketCommentApprovalCommand command;
        private PreparedTicketCommentApproval prepared;

        @Override
        public PreparedTicketCommentApproval prepareProposal(Long tenantId, TicketCommentApprovalCommand command) {
            this.command = command;
            return prepared;
        }
    }

    private static final class NoopApprovalImportSelectiveReplayPort implements ApprovalImportSelectiveReplayPort {

        @Override
        public PreparedImportSelectiveReplayApproval prepareProposal(Long tenantId, ImportSelectiveReplayApprovalCommand command) {
            return new PreparedImportSelectiveReplayApproval(
                    command.sourceJobId(),
                    command.errorCodes(),
                    command.sourceInteractionId(),
                    command.proposalReason(),
                    "{\"sourceJobId\":7001,\"errorCodes\":[\"UNKNOWN_ROLE\"]}",
                    ApprovalPendingRequestKeyPolicy.importJobSelectiveReplayKey(tenantId, command.sourceJobId(), command.errorCodes())
            );
        }
    }

    private static final class NoopApprovalTicketCommentProposalPort implements ApprovalTicketCommentProposalPort {

        @Override
        public PreparedTicketCommentApproval prepareProposal(Long tenantId, TicketCommentApprovalCommand command) {
            return new PreparedTicketCommentApproval(
                    command.ticketId(),
                    command.commentContent(),
                    command.sourceInteractionId(),
                    "{\"commentContent\":\"Reply draft content\"}",
                    ApprovalPendingRequestKeyPolicy.ticketCommentCreateKey(tenantId, command.ticketId(), command.commentContent())
            );
        }
    }
}
