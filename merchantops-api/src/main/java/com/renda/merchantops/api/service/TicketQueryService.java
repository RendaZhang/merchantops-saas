package com.renda.merchantops.api.service;

import com.renda.merchantops.api.dto.ticket.query.TicketCommentResponse;
import com.renda.merchantops.api.dto.ticket.query.TicketDetailResponse;
import com.renda.merchantops.api.dto.ticket.query.TicketListItemResponse;
import com.renda.merchantops.api.dto.ticket.query.TicketOperationLogResponse;
import com.renda.merchantops.api.dto.ticket.query.TicketPageQuery;
import com.renda.merchantops.api.dto.ticket.query.TicketPageResponse;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TicketQueryService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 100;

    private final TicketRepository ticketRepository;
    private final TicketCommentRepository ticketCommentRepository;
    private final TicketOperationLogRepository ticketOperationLogRepository;
    private final UserRepository userRepository;

    public TicketPageResponse pageTickets(Long tenantId, TicketPageQuery query) {
        TicketPageQuery normalizedQuery = normalizeQuery(query);
        PageRequest pageable = PageRequest.of(
                normalizedQuery.getPage(),
                normalizedQuery.getSize(),
                Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.desc("id"))
        );

        Page<TicketEntity> resultPage = ticketRepository.pageByTenantAndFilters(
                tenantId,
                normalizedQuery.getStatus(),
                normalizedQuery.getAssigneeId(),
                normalizedQuery.getKeyword(),
                Boolean.TRUE.equals(normalizedQuery.getUnassignedOnly()),
                pageable
        );

        Map<Long, String> usernamesById = loadUsernamesById(
                tenantId,
                resultPage.getContent().stream()
                        .map(TicketEntity::getAssigneeId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toCollection(LinkedHashSet::new))
        );

        return new TicketPageResponse(
                resultPage.getContent().stream()
                        .map(ticket -> toListItemResponse(ticket, usernamesById))
                        .toList(),
                resultPage.getNumber(),
                resultPage.getSize(),
                resultPage.getTotalElements(),
                resultPage.getTotalPages()
        );
    }

    public TicketDetailResponse getTicketDetail(Long tenantId, Long ticketId) {
        TicketEntity ticket = ticketRepository.findByIdAndTenantId(ticketId, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "ticket not found"));

        List<TicketCommentEntity> comments = ticketCommentRepository.findAllByTicketIdAndTenantIdOrderByIdAsc(ticketId, tenantId);
        List<TicketOperationLogEntity> operationLogs = ticketOperationLogRepository.findAllByTicketIdAndTenantIdOrderByIdAsc(ticketId, tenantId);

        Set<Long> userIds = new LinkedHashSet<>();
        if (ticket.getAssigneeId() != null) {
            userIds.add(ticket.getAssigneeId());
        }
        userIds.add(ticket.getCreatedBy());
        comments.stream().map(TicketCommentEntity::getCreatedBy).forEach(userIds::add);
        operationLogs.stream().map(TicketOperationLogEntity::getOperatorId).forEach(userIds::add);

        Map<Long, String> usernamesById = loadUsernamesById(tenantId, userIds);

        return new TicketDetailResponse(
                ticket.getId(),
                ticket.getTenantId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getStatus(),
                ticket.getAssigneeId(),
                usernamesById.get(ticket.getAssigneeId()),
                ticket.getCreatedBy(),
                usernamesById.get(ticket.getCreatedBy()),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                comments.stream().map(comment -> new TicketCommentResponse(
                        comment.getId(),
                        comment.getTicketId(),
                        comment.getContent(),
                        comment.getCreatedBy(),
                        usernamesById.get(comment.getCreatedBy()),
                        comment.getCreatedAt()
                )).toList(),
                operationLogs.stream().map(log -> new TicketOperationLogResponse(
                        log.getId(),
                        log.getOperationType(),
                        log.getDetail(),
                        log.getOperatorId(),
                        usernamesById.get(log.getOperatorId()),
                        log.getCreatedAt()
                )).toList()
        );
    }

    private TicketPageQuery normalizeQuery(TicketPageQuery query) {
        TicketPageQuery normalized = query == null ? new TicketPageQuery() : query;
        normalized.setPage(normalizePage(query));
        normalized.setSize(normalizeSize(query));
        normalized.setStatus(normalizeFilter(normalized.getStatus()));
        normalized.setKeyword(normalizeFilter(normalized.getKeyword()));
        normalized.setUnassignedOnly(Boolean.TRUE.equals(normalized.getUnassignedOnly()));
        if (normalized.getAssigneeId() != null && Boolean.TRUE.equals(normalized.getUnassignedOnly())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "assigneeId cannot be combined with unassignedOnly=true");
        }
        return normalized;
    }

    private int normalizePage(TicketPageQuery query) {
        if (query == null || query.getPage() == null || query.getPage() < 0) {
            return DEFAULT_PAGE;
        }
        return query.getPage();
    }

    private int normalizeSize(TicketPageQuery query) {
        if (query == null || query.getSize() == null || query.getSize() <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(query.getSize(), MAX_SIZE);
    }

    private String normalizeFilter(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private TicketListItemResponse toListItemResponse(TicketEntity ticket, Map<Long, String> usernamesById) {
        Long assigneeId = ticket.getAssigneeId();
        return new TicketListItemResponse(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getStatus(),
                assigneeId,
                assigneeId == null ? null : usernamesById.get(assigneeId),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
    }

    private Map<Long, String> loadUsernamesById(Long tenantId, Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllByTenantIdAndIdIn(tenantId, userIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, UserEntity::getUsername, (left, right) -> left));
    }
}
