package com.renda.merchantops.api.service;

import com.renda.merchantops.api.dto.ticket.command.TicketAssigneeUpdateCommand;
import com.renda.merchantops.api.dto.ticket.command.TicketCommentCreateCommand;
import com.renda.merchantops.api.dto.ticket.command.TicketCreateCommand;
import com.renda.merchantops.api.dto.ticket.command.TicketStatusUpdateCommand;
import com.renda.merchantops.api.dto.ticket.command.TicketWriteResponse;
import com.renda.merchantops.api.dto.ticket.query.TicketCommentResponse;
import com.renda.merchantops.common.exception.BizException;
import com.renda.merchantops.common.exception.ErrorCode;
import com.renda.merchantops.infra.persistence.entity.TicketCommentEntity;
import com.renda.merchantops.infra.persistence.entity.TicketEntity;
import com.renda.merchantops.infra.persistence.entity.TicketOperationLogEntity;
import com.renda.merchantops.infra.persistence.entity.UserEntity;
import com.renda.merchantops.infra.repository.TicketCommentRepository;
import com.renda.merchantops.infra.repository.TicketOperationLogRepository;
import com.renda.merchantops.infra.repository.TicketRepository;
import com.renda.merchantops.infra.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TicketCommandService {

    private static final Set<String> ALLOWED_STATUSES = Set.of("OPEN", "IN_PROGRESS", "CLOSED");

    private final TicketRepository ticketRepository;
    private final TicketCommentRepository ticketCommentRepository;
    private final TicketOperationLogRepository ticketOperationLogRepository;
    private final UserRepository userRepository;

    @Transactional
    public TicketWriteResponse createTicket(Long tenantId, Long operatorId, String requestId, TicketCreateCommand command) {
        Long resolvedOperatorId = requireOperatorId(operatorId);
        String resolvedRequestId = requireRequestId(requestId);
        LocalDateTime now = LocalDateTime.now();

        TicketEntity ticket = new TicketEntity();
        ticket.setTenantId(tenantId);
        ticket.setTitle(normalizeRequiredText(command == null ? null : command.getTitle(), "title"));
        ticket.setDescription(normalizeOptionalText(command == null ? null : command.getDescription()));
        ticket.setStatus("OPEN");
        ticket.setCreatedBy(resolvedOperatorId);
        ticket.setRequestId(resolvedRequestId);
        ticket.setCreatedAt(now);
        ticket.setUpdatedAt(now);

        TicketEntity savedTicket = ticketRepository.save(ticket);
        appendOperationLog(savedTicket.getId(), tenantId, resolvedOperatorId, resolvedRequestId, "CREATED", "ticket created", now);

        return toWriteResponse(savedTicket, null);
    }

    @Transactional
    public TicketWriteResponse assignTicket(Long tenantId,
                                            Long operatorId,
                                            String requestId,
                                            Long ticketId,
                                            TicketAssigneeUpdateCommand command) {
        TicketEntity ticket = requireTenantTicket(tenantId, ticketId);
        Long resolvedOperatorId = requireOperatorId(operatorId);
        String resolvedRequestId = requireRequestId(requestId);
        UserEntity assignee = requireActiveTenantUser(tenantId, command == null ? null : command.getAssigneeId());

        LocalDateTime now = LocalDateTime.now();
        ticket.setAssigneeId(assignee.getId());
        ticket.setUpdatedAt(now);
        TicketEntity savedTicket = ticketRepository.save(ticket);

        appendOperationLog(savedTicket.getId(), tenantId, resolvedOperatorId, resolvedRequestId, "ASSIGNED", "assigned to " + assignee.getUsername(), now);
        return toWriteResponse(savedTicket, assignee.getUsername());
    }

    @Transactional
    public TicketWriteResponse updateStatus(Long tenantId,
                                            Long operatorId,
                                            String requestId,
                                            Long ticketId,
                                            TicketStatusUpdateCommand command) {
        TicketEntity ticket = requireTenantTicket(tenantId, ticketId);
        Long resolvedOperatorId = requireOperatorId(operatorId);
        String resolvedRequestId = requireRequestId(requestId);
        String nextStatus = normalizeAllowedStatus(command == null ? null : command.getStatus());
        validateStatusTransition(ticket.getStatus(), nextStatus);

        LocalDateTime now = LocalDateTime.now();
        String previousStatus = ticket.getStatus();
        ticket.setStatus(nextStatus);
        ticket.setUpdatedAt(now);
        TicketEntity savedTicket = ticketRepository.save(ticket);

        appendOperationLog(
                savedTicket.getId(),
                tenantId,
                resolvedOperatorId,
                resolvedRequestId,
                "STATUS_CHANGED",
                "status changed from " + previousStatus + " to " + nextStatus,
                now
        );
        return toWriteResponse(savedTicket, loadUsername(tenantId, savedTicket.getAssigneeId()));
    }

    @Transactional
    public TicketCommentResponse addComment(Long tenantId,
                                            Long operatorId,
                                            String requestId,
                                            Long ticketId,
                                            TicketCommentCreateCommand command) {
        TicketEntity ticket = requireTenantTicket(tenantId, ticketId);
        Long resolvedOperatorId = requireOperatorId(operatorId);
        String resolvedRequestId = requireRequestId(requestId);
        UserEntity operator = requireTenantUser(tenantId, resolvedOperatorId);
        LocalDateTime now = LocalDateTime.now();

        TicketCommentEntity comment = new TicketCommentEntity();
        comment.setTenantId(tenantId);
        comment.setTicketId(ticket.getId());
        comment.setContent(normalizeRequiredText(command == null ? null : command.getContent(), "content"));
        comment.setCreatedBy(resolvedOperatorId);
        comment.setRequestId(resolvedRequestId);
        comment.setCreatedAt(now);

        TicketCommentEntity savedComment = ticketCommentRepository.save(comment);
        ticket.setUpdatedAt(now);
        ticketRepository.save(ticket);

        appendOperationLog(ticket.getId(), tenantId, resolvedOperatorId, resolvedRequestId, "COMMENTED", "comment added", now);

        return new TicketCommentResponse(
                savedComment.getId(),
                savedComment.getTicketId(),
                savedComment.getContent(),
                savedComment.getCreatedBy(),
                operator.getUsername(),
                savedComment.getCreatedAt()
        );
    }

    private TicketEntity requireTenantTicket(Long tenantId, Long ticketId) {
        return ticketRepository.findByIdAndTenantId(ticketId, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "ticket not found"));
    }

    private UserEntity requireTenantUser(Long tenantId, Long userId) {
        if (userId == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "userId must not be null");
        }
        return userRepository.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.BAD_REQUEST, "user must exist in current tenant"));
    }

    private UserEntity requireActiveTenantUser(Long tenantId, Long userId) {
        UserEntity user = requireTenantUser(tenantId, userId);
        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
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
            case "CLOSED" -> false;
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
        if (!StringUtils.hasText(value)) {
            throw new BizException(ErrorCode.BAD_REQUEST, fieldName + " must not be blank");
        }
        return value.trim();
    }

    private String normalizeOptionalText(String value) {
        if (!StringUtils.hasText(value)) {
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
        if (!StringUtils.hasText(requestId)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "request id missing");
        }
        return requestId;
    }

    private void appendOperationLog(Long ticketId,
                                    Long tenantId,
                                    Long operatorId,
                                    String requestId,
                                    String operationType,
                                    String detail,
                                    LocalDateTime now) {
        TicketOperationLogEntity operationLog = new TicketOperationLogEntity();
        operationLog.setTicketId(ticketId);
        operationLog.setTenantId(tenantId);
        operationLog.setOperatorId(operatorId);
        operationLog.setRequestId(requestId);
        operationLog.setOperationType(operationType);
        operationLog.setDetail(detail);
        operationLog.setCreatedAt(now);
        ticketOperationLogRepository.save(operationLog);
    }

    private TicketWriteResponse toWriteResponse(TicketEntity ticket, String assigneeUsername) {
        return new TicketWriteResponse(
                ticket.getId(),
                ticket.getTenantId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getStatus(),
                ticket.getAssigneeId(),
                assigneeUsername,
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
    }

    private String loadUsername(Long tenantId, Long userId) {
        if (userId == null) {
            return null;
        }
        return userRepository.findByIdAndTenantId(userId, tenantId)
                .map(UserEntity::getUsername)
                .orElse(null);
    }
}
