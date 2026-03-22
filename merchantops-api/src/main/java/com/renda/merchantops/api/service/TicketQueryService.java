package com.renda.merchantops.api.service;

import com.renda.merchantops.api.ai.TicketAiPromptContext;
import com.renda.merchantops.api.ai.TicketAiPromptSupport;
import com.renda.merchantops.api.dto.ticket.query.TicketAiInteractionListItemResponse;
import com.renda.merchantops.api.dto.ticket.query.TicketAiInteractionPageQuery;
import com.renda.merchantops.api.dto.ticket.query.TicketAiInteractionPageResponse;
import com.renda.merchantops.api.dto.ticket.query.TicketCommentResponse;
import com.renda.merchantops.api.dto.ticket.query.TicketDetailResponse;
import com.renda.merchantops.api.dto.ticket.query.TicketListItemResponse;
import com.renda.merchantops.api.dto.ticket.query.TicketOperationLogResponse;
import com.renda.merchantops.api.dto.ticket.query.TicketPageQuery;
import com.renda.merchantops.api.dto.ticket.query.TicketPageResponse;
import com.renda.merchantops.common.exception.BizException;
import com.renda.merchantops.common.exception.ErrorCode;
import com.renda.merchantops.infra.persistence.entity.AiInteractionRecordEntity;
import com.renda.merchantops.infra.persistence.entity.TicketCommentEntity;
import com.renda.merchantops.infra.persistence.entity.TicketEntity;
import com.renda.merchantops.infra.persistence.entity.TicketOperationLogEntity;
import com.renda.merchantops.infra.persistence.entity.UserEntity;
import com.renda.merchantops.infra.repository.AiInteractionRecordRepository;
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

import java.util.ArrayList;
import java.util.Comparator;
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
    private static final String ENTITY_TYPE_TICKET = "TICKET";

    private final TicketRepository ticketRepository;
    private final AiInteractionRecordRepository aiInteractionRecordRepository;
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

    public TicketAiInteractionPageResponse pageTicketAiInteractions(Long tenantId,
                                                                    Long ticketId,
                                                                    TicketAiInteractionPageQuery query) {
        requireTicket(tenantId, ticketId);
        TicketAiInteractionPageQuery normalizedQuery = normalizeQuery(query);
        PageRequest pageable = PageRequest.of(
                normalizePage(normalizedQuery.getPage()),
                normalizeSize(normalizedQuery.getSize()),
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))
        );

        Page<AiInteractionRecordEntity> resultPage = aiInteractionRecordRepository.searchPageByTenantIdAndEntity(
                tenantId,
                ENTITY_TYPE_TICKET,
                ticketId,
                normalizeFilter(normalizedQuery.getInteractionType()),
                normalizeFilter(normalizedQuery.getStatus()),
                pageable
        );

        return new TicketAiInteractionPageResponse(
                resultPage.getContent().stream()
                        .map(this::toAiInteractionListItemResponse)
                        .toList(),
                resultPage.getNumber(),
                resultPage.getSize(),
                resultPage.getTotalElements(),
                resultPage.getTotalPages()
        );
    }

    public TicketDetailResponse getTicketDetail(Long tenantId, Long ticketId) {
        TicketEntity ticket = requireTicket(tenantId, ticketId);

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

    public TicketAiPromptContext getTicketPromptContext(Long tenantId, Long ticketId) {
        TicketEntity ticket = requireTicket(tenantId, ticketId);

        WindowedComments comments = loadRecentComments(ticketId, tenantId);
        WindowedOperationLogs operationLogs = loadRecentOperationLogs(ticketId, tenantId);

        Set<Long> userIds = new LinkedHashSet<>();
        if (ticket.getAssigneeId() != null) {
            userIds.add(ticket.getAssigneeId());
        }
        userIds.add(ticket.getCreatedBy());
        comments.items().stream().map(TicketCommentEntity::getCreatedBy).forEach(userIds::add);
        operationLogs.items().stream().map(TicketOperationLogEntity::getOperatorId).forEach(userIds::add);

        Map<Long, String> usernamesById = loadUsernamesById(tenantId, userIds);

        return new TicketAiPromptContext(
                ticket.getId(),
                ticket.getTenantId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getStatus(),
                usernamesById.get(ticket.getAssigneeId()),
                usernamesById.get(ticket.getCreatedBy()),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                comments.items().stream()
                        .map(comment -> new TicketAiPromptContext.Comment(
                                comment.getId(),
                                comment.getContent(),
                                usernamesById.get(comment.getCreatedBy()),
                                comment.getCreatedAt()
                        ))
                        .toList(),
                comments.olderItemsOmitted(),
                operationLogs.items().stream()
                        .map(log -> new TicketAiPromptContext.OperationLog(
                                log.getId(),
                                log.getOperationType(),
                                log.getDetail(),
                                usernamesById.get(log.getOperatorId()),
                                log.getCreatedAt()
                        ))
                        .toList(),
                operationLogs.olderItemsOmitted()
        );
    }

    private TicketPageQuery normalizeQuery(TicketPageQuery query) {
        TicketPageQuery normalized = query == null ? new TicketPageQuery() : query;
        normalized.setPage(normalizePage(normalized.getPage()));
        normalized.setSize(normalizeSize(normalized.getSize()));
        normalized.setStatus(normalizeFilter(normalized.getStatus()));
        normalized.setKeyword(normalizeFilter(normalized.getKeyword()));
        normalized.setUnassignedOnly(Boolean.TRUE.equals(normalized.getUnassignedOnly()));
        if (normalized.getAssigneeId() != null && Boolean.TRUE.equals(normalized.getUnassignedOnly())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "assigneeId cannot be combined with unassignedOnly=true");
        }
        return normalized;
    }

    private TicketAiInteractionPageQuery normalizeQuery(TicketAiInteractionPageQuery query) {
        TicketAiInteractionPageQuery normalized = query == null ? new TicketAiInteractionPageQuery() : query;
        normalized.setPage(normalizePage(normalized.getPage()));
        normalized.setSize(normalizeSize(normalized.getSize()));
        normalized.setInteractionType(normalizeFilter(normalized.getInteractionType()));
        normalized.setStatus(normalizeFilter(normalized.getStatus()));
        return normalized;
    }

    private int normalizePage(Integer page) {
        if (page == null || page < 0) {
            return DEFAULT_PAGE;
        }
        return page;
    }

    private int normalizeSize(Integer size) {
        if (size == null || size <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
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

    private TicketAiInteractionListItemResponse toAiInteractionListItemResponse(AiInteractionRecordEntity record) {
        return new TicketAiInteractionListItemResponse(
                record.getId(),
                record.getInteractionType(),
                record.getStatus(),
                record.getOutputSummary(),
                record.getPromptVersion(),
                record.getModelId(),
                record.getLatencyMs(),
                record.getRequestId(),
                record.getUsagePromptTokens(),
                record.getUsageCompletionTokens(),
                record.getUsageTotalTokens(),
                record.getUsageCostMicros(),
                record.getCreatedAt()
        );
    }

    private Map<Long, String> loadUsernamesById(Long tenantId, Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllByTenantIdAndIdIn(tenantId, userIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, UserEntity::getUsername, (left, right) -> left));
    }

    private TicketEntity requireTicket(Long tenantId, Long ticketId) {
        return ticketRepository.findByIdAndTenantId(ticketId, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "ticket not found"));
    }

    private WindowedComments loadRecentComments(Long ticketId, Long tenantId) {
        List<TicketCommentEntity> latestComments = ticketCommentRepository.findByTicketIdAndTenantIdOrderByIdDesc(
                ticketId,
                tenantId,
                PageRequest.of(0, TicketAiPromptSupport.COMMENT_HISTORY_LIMIT + 1)
        );
        boolean olderItemsOmitted = latestComments.size() > TicketAiPromptSupport.COMMENT_HISTORY_LIMIT;
        List<TicketCommentEntity> window = new ArrayList<>(
                latestComments.subList(0, Math.min(latestComments.size(), TicketAiPromptSupport.COMMENT_HISTORY_LIMIT))
        );
        window.sort(Comparator.comparing(TicketCommentEntity::getId));
        return new WindowedComments(List.copyOf(window), olderItemsOmitted);
    }

    private WindowedOperationLogs loadRecentOperationLogs(Long ticketId, Long tenantId) {
        List<TicketOperationLogEntity> latestOperationLogs = ticketOperationLogRepository.findByTicketIdAndTenantIdOrderByIdDesc(
                ticketId,
                tenantId,
                PageRequest.of(0, TicketAiPromptSupport.OPERATION_LOG_HISTORY_LIMIT + 1)
        );
        boolean olderItemsOmitted = latestOperationLogs.size() > TicketAiPromptSupport.OPERATION_LOG_HISTORY_LIMIT;
        List<TicketOperationLogEntity> window = new ArrayList<>(
                latestOperationLogs.subList(0, Math.min(latestOperationLogs.size(), TicketAiPromptSupport.OPERATION_LOG_HISTORY_LIMIT))
        );
        window.sort(Comparator.comparing(TicketOperationLogEntity::getId));
        return new WindowedOperationLogs(List.copyOf(window), olderItemsOmitted);
    }

    private record WindowedComments(
            List<TicketCommentEntity> items,
            boolean olderItemsOmitted
    ) {
    }

    private record WindowedOperationLogs(
            List<TicketOperationLogEntity> items,
            boolean olderItemsOmitted
    ) {
    }
}
