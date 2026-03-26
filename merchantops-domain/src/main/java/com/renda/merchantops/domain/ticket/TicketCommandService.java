package com.renda.merchantops.domain.ticket;

import com.renda.merchantops.domain.shared.error.BizException;
import com.renda.merchantops.domain.shared.error.ErrorCode;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class TicketCommandService implements TicketCommandUseCase {

    private static final Set<String> ALLOWED_STATUSES = Set.of("OPEN", "IN_PROGRESS", "CLOSED");

    private final TicketCommandPort ticketCommandPort;
    private final TicketAuditPort ticketAuditPort;

    public TicketCommandService(TicketCommandPort ticketCommandPort, TicketAuditPort ticketAuditPort) {
        this.ticketCommandPort = ticketCommandPort;
        this.ticketAuditPort = ticketAuditPort;
    }

    @Override
    public TicketWriteResult createTicket(Long tenantId, Long operatorId, String requestId, CreateTicketCommand command) {
        Long resolvedOperatorId = requireOperatorId(operatorId);
        String resolvedRequestId = requireRequestId(requestId);
        LocalDateTime now = LocalDateTime.now();

        ManagedTicket savedTicket = ticketCommandPort.createTicket(new NewTicketDraft(
                tenantId,
                normalizeRequiredText(command == null ? null : command.title(), "title"),
                normalizeOptionalText(command == null ? null : command.description()),
                "OPEN",
                resolvedOperatorId,
                resolvedRequestId,
                now,
                now
        ));
        ticketCommandPort.appendOperationLog(new NewTicketOperationLogDraft(
                savedTicket.id(),
                tenantId,
                resolvedOperatorId,
                resolvedRequestId,
                "CREATED",
                "ticket created",
                now
        ));
        ticketAuditPort.recordEvent(
                tenantId,
                "TICKET",
                savedTicket.id(),
                "TICKET_CREATED",
                resolvedOperatorId,
                resolvedRequestId,
                null,
                snapshotTicket(savedTicket)
        );
        return toWriteResult(savedTicket, null);
    }

    @Override
    public TicketWriteResult assignTicket(Long tenantId,
                                          Long operatorId,
                                          String requestId,
                                          Long ticketId,
                                          AssignTicketCommand command) {
        ManagedTicket ticket = requireTenantTicket(tenantId, ticketId);
        Long resolvedOperatorId = requireOperatorId(operatorId);
        String resolvedRequestId = requireRequestId(requestId);
        TicketUserAccount assignee = requireActiveTenantUser(tenantId, command == null ? null : command.assigneeId());

        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> before = snapshotTicket(ticket);
        ManagedTicket savedTicket = ticketCommandPort.saveTicket(new TicketUpdateDraft(
                ticket.id(),
                ticket.tenantId(),
                ticket.title(),
                ticket.description(),
                ticket.status(),
                assignee.id(),
                ticket.createdBy(),
                ticket.requestId(),
                ticket.createdAt(),
                now
        ));
        ticketCommandPort.appendOperationLog(new NewTicketOperationLogDraft(
                savedTicket.id(),
                tenantId,
                resolvedOperatorId,
                resolvedRequestId,
                "ASSIGNED",
                "assigned to " + assignee.username(),
                now
        ));
        ticketAuditPort.recordEvent(
                tenantId,
                "TICKET",
                savedTicket.id(),
                "TICKET_ASSIGNED",
                resolvedOperatorId,
                resolvedRequestId,
                before,
                snapshotTicket(savedTicket)
        );
        return toWriteResult(savedTicket, assignee.username());
    }

    @Override
    public TicketWriteResult updateStatus(Long tenantId,
                                          Long operatorId,
                                          String requestId,
                                          Long ticketId,
                                          UpdateTicketStatusCommand command) {
        ManagedTicket ticket = requireTenantTicket(tenantId, ticketId);
        Long resolvedOperatorId = requireOperatorId(operatorId);
        String resolvedRequestId = requireRequestId(requestId);
        String nextStatus = normalizeAllowedStatus(command == null ? null : command.status());
        validateStatusTransition(ticket.status(), nextStatus);

        LocalDateTime now = LocalDateTime.now();
        String previousStatus = ticket.status();
        Map<String, Object> before = snapshotTicket(ticket);
        ManagedTicket savedTicket = ticketCommandPort.saveTicket(new TicketUpdateDraft(
                ticket.id(),
                ticket.tenantId(),
                ticket.title(),
                ticket.description(),
                nextStatus,
                ticket.assigneeId(),
                ticket.createdBy(),
                ticket.requestId(),
                ticket.createdAt(),
                now
        ));
        ticketCommandPort.appendOperationLog(new NewTicketOperationLogDraft(
                savedTicket.id(),
                tenantId,
                resolvedOperatorId,
                resolvedRequestId,
                "STATUS_CHANGED",
                "status changed from " + previousStatus + " to " + nextStatus,
                now
        ));
        ticketAuditPort.recordEvent(
                tenantId,
                "TICKET",
                savedTicket.id(),
                "TICKET_STATUS_UPDATED",
                resolvedOperatorId,
                resolvedRequestId,
                before,
                snapshotTicket(savedTicket)
        );
        return toWriteResult(savedTicket, loadUsername(tenantId, savedTicket.assigneeId()));
    }

    @Override
    public TicketCommentResult addComment(Long tenantId,
                                          Long operatorId,
                                          String requestId,
                                          Long ticketId,
                                          AddTicketCommentCommand command) {
        ManagedTicket ticket = requireTenantTicket(tenantId, ticketId);
        Long resolvedOperatorId = requireOperatorId(operatorId);
        String resolvedRequestId = requireRequestId(requestId);
        TicketUserAccount operator = requireTenantUser(tenantId, resolvedOperatorId);
        LocalDateTime now = LocalDateTime.now();

        StoredTicketComment savedComment = ticketCommandPort.createComment(new NewTicketCommentDraft(
                tenantId,
                ticket.id(),
                normalizeRequiredText(command == null ? null : command.content(), "content"),
                resolvedOperatorId,
                resolvedRequestId,
                now
        ));
        ticketCommandPort.saveTicket(new TicketUpdateDraft(
                ticket.id(),
                ticket.tenantId(),
                ticket.title(),
                ticket.description(),
                ticket.status(),
                ticket.assigneeId(),
                ticket.createdBy(),
                ticket.requestId(),
                ticket.createdAt(),
                now
        ));
        ticketCommandPort.appendOperationLog(new NewTicketOperationLogDraft(
                ticket.id(),
                tenantId,
                resolvedOperatorId,
                resolvedRequestId,
                "COMMENTED",
                "comment added",
                now
        ));
        ticketAuditPort.recordEvent(
                tenantId,
                "TICKET",
                ticket.id(),
                "TICKET_COMMENT_ADDED",
                resolvedOperatorId,
                resolvedRequestId,
                null,
                Map.of(
                        "commentId", savedComment.id(),
                        "content", savedComment.content()
                )
        );
        return new TicketCommentResult(
                savedComment.id(),
                savedComment.ticketId(),
                savedComment.content(),
                savedComment.createdBy(),
                operator.username(),
                savedComment.createdAt()
        );
    }

    private ManagedTicket requireTenantTicket(Long tenantId, Long ticketId) {
        return ticketCommandPort.findManagedTicket(tenantId, ticketId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "ticket not found"));
    }

    private TicketUserAccount requireTenantUser(Long tenantId, Long userId) {
        if (userId == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "userId must not be null");
        }
        return ticketCommandPort.findTenantUser(tenantId, userId)
                .orElseThrow(() -> new BizException(ErrorCode.BAD_REQUEST, "user must exist in current tenant"));
    }

    private TicketUserAccount requireActiveTenantUser(Long tenantId, Long userId) {
        TicketUserAccount user = requireTenantUser(tenantId, userId);
        if (!"ACTIVE".equalsIgnoreCase(user.status())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "assignee must be active");
        }
        return user;
    }

    private void validateStatusTransition(String currentStatus, String nextStatus) {
        if (currentStatus == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "ticket status transition is not allowed");
        }
        if (currentStatus.equals(nextStatus)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "ticket is already in status " + nextStatus);
        }
        boolean allowed = switch (currentStatus) {
            case "OPEN" -> "IN_PROGRESS".equals(nextStatus) || "CLOSED".equals(nextStatus);
            case "IN_PROGRESS" -> "CLOSED".equals(nextStatus);
            case "CLOSED" -> "OPEN".equals(nextStatus);
            default -> false;
        };
        if (!allowed) {
            throw new BizException(ErrorCode.BAD_REQUEST, "ticket status transition is not allowed");
        }
    }

    private String normalizeAllowedStatus(String value) {
        String status = normalizeRequiredText(value, "status");
        if (!ALLOWED_STATUSES.contains(status)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "status must be one of OPEN, IN_PROGRESS, CLOSED");
        }
        return status;
    }

    private String normalizeRequiredText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BizException(ErrorCode.BAD_REQUEST, fieldName + " must not be blank");
        }
        return value.trim();
    }

    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private Long requireOperatorId(Long operatorId) {
        if (operatorId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "user context missing");
        }
        return operatorId;
    }

    private String requireRequestId(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "request id missing");
        }
        return requestId.trim();
    }

    private String loadUsername(Long tenantId, Long userId) {
        if (userId == null) {
            return null;
        }
        return ticketCommandPort.findTenantUser(tenantId, userId)
                .map(TicketUserAccount::username)
                .orElse(null);
    }

    private TicketWriteResult toWriteResult(ManagedTicket ticket, String assigneeUsername) {
        return new TicketWriteResult(
                ticket.id(),
                ticket.tenantId(),
                ticket.title(),
                ticket.description(),
                ticket.status(),
                ticket.assigneeId(),
                assigneeUsername,
                ticket.createdAt(),
                ticket.updatedAt()
        );
    }

    private Map<String, Object> snapshotTicket(ManagedTicket ticket) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", ticket.id());
        snapshot.put("tenantId", ticket.tenantId());
        snapshot.put("title", ticket.title());
        snapshot.put("description", ticket.description());
        snapshot.put("status", ticket.status());
        snapshot.put("assigneeId", ticket.assigneeId());
        return snapshot;
    }
}
