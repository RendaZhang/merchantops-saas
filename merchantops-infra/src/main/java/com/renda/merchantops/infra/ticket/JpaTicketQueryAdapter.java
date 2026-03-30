package com.renda.merchantops.infra.ticket;

import com.renda.merchantops.domain.ticket.TicketAiInteractionItem;
import com.renda.merchantops.domain.ticket.TicketAiInteractionPageCriteria;
import com.renda.merchantops.domain.ticket.TicketAiInteractionPageResult;
import com.renda.merchantops.domain.ticket.TicketCommentView;
import com.renda.merchantops.domain.ticket.TicketDetail;
import com.renda.merchantops.domain.ticket.TicketListItem;
import com.renda.merchantops.domain.ticket.TicketOperationLogView;
import com.renda.merchantops.domain.ticket.TicketPageCriteria;
import com.renda.merchantops.domain.ticket.TicketPageResult;
import com.renda.merchantops.domain.ticket.TicketPromptContext;
import com.renda.merchantops.domain.ticket.TicketPromptWindowPolicy;
import com.renda.merchantops.domain.ticket.TicketQueryPort;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class JpaTicketQueryAdapter implements TicketQueryPort {

    private static final String ENTITY_TYPE_TICKET = "TICKET";

    private final TicketRepository ticketRepository;
    private final AiInteractionRecordRepository aiInteractionRecordRepository;
    private final TicketCommentRepository ticketCommentRepository;
    private final TicketOperationLogRepository ticketOperationLogRepository;
    private final UserRepository userRepository;

    public JpaTicketQueryAdapter(TicketRepository ticketRepository,
                                 AiInteractionRecordRepository aiInteractionRecordRepository,
                                 TicketCommentRepository ticketCommentRepository,
                                 TicketOperationLogRepository ticketOperationLogRepository,
                                 UserRepository userRepository) {
        this.ticketRepository = ticketRepository;
        this.aiInteractionRecordRepository = aiInteractionRecordRepository;
        this.ticketCommentRepository = ticketCommentRepository;
        this.ticketOperationLogRepository = ticketOperationLogRepository;
        this.userRepository = userRepository;
    }

    @Override
    public TicketPageResult pageTickets(Long tenantId, TicketPageCriteria criteria) {
        Page<TicketEntity> resultPage = ticketRepository.pageByTenantAndFilters(
                tenantId,
                criteria.status(),
                criteria.assigneeId(),
                criteria.keyword(),
                criteria.unassignedOnly(),
                PageRequest.of(
                        criteria.page(),
                        criteria.size(),
                        Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.desc("id"))
                )
        );
        // Resolve usernames in one lookup so page rendering does not devolve into per-ticket
        // repository calls and so unassigned tickets still map cleanly to null usernames.
        Map<Long, String> usernamesById = loadUsernamesById(
                tenantId,
                resultPage.getContent().stream()
                        .map(TicketEntity::getAssigneeId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toCollection(LinkedHashSet::new))
        );
        return new TicketPageResult(
                resultPage.getContent().stream()
                        .map(ticket -> toListItem(ticket, usernamesById))
                        .toList(),
                resultPage.getNumber(),
                resultPage.getSize(),
                resultPage.getTotalElements(),
                resultPage.getTotalPages()
        );
    }

    @Override
    public boolean ticketExists(Long tenantId, Long ticketId) {
        return ticketRepository.findByIdAndTenantId(ticketId, tenantId).isPresent();
    }

    @Override
    public TicketAiInteractionPageResult pageTicketAiInteractions(Long tenantId,
                                                                  Long ticketId,
                                                                  TicketAiInteractionPageCriteria criteria) {
        Page<AiInteractionRecordEntity> resultPage = aiInteractionRecordRepository.searchPageByTenantIdAndEntity(
                tenantId,
                ENTITY_TYPE_TICKET,
                ticketId,
                criteria.interactionType(),
                criteria.status(),
                PageRequest.of(
                        criteria.page(),
                        criteria.size(),
                        Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))
                )
        );
        return new TicketAiInteractionPageResult(
                resultPage.getContent().stream().map(this::toAiInteractionItem).toList(),
                resultPage.getNumber(),
                resultPage.getSize(),
                resultPage.getTotalElements(),
                resultPage.getTotalPages()
        );
    }

    @Override
    public Optional<TicketAiInteractionItem> findTicketAiInteraction(Long tenantId, Long ticketId, Long interactionId) {
        return aiInteractionRecordRepository.findByIdAndTenantIdAndEntityTypeAndEntityId(
                        interactionId,
                        tenantId,
                        ENTITY_TYPE_TICKET,
                        ticketId
                )
                .map(this::toAiInteractionItem);
    }

    @Override
    public Optional<TicketDetail> findTicketDetail(Long tenantId, Long ticketId) {
        return ticketRepository.findByIdAndTenantId(ticketId, tenantId)
                .map(ticket -> {
                    List<TicketCommentEntity> comments = ticketCommentRepository.findAllByTicketIdAndTenantIdOrderByIdAsc(ticketId, tenantId);
                    List<TicketOperationLogEntity> operationLogs =
                            ticketOperationLogRepository.findAllByTicketIdAndTenantIdOrderByIdAsc(ticketId, tenantId);

                    Set<Long> userIds = new LinkedHashSet<>();
                    if (ticket.getAssigneeId() != null) {
                        userIds.add(ticket.getAssigneeId());
                    }
                    userIds.add(ticket.getCreatedBy());
                    comments.stream().map(TicketCommentEntity::getCreatedBy).forEach(userIds::add);
                    operationLogs.stream().map(TicketOperationLogEntity::getOperatorId).forEach(userIds::add);
                    Map<Long, String> usernamesById = loadUsernamesById(tenantId, userIds);

                    return new TicketDetail(
                            ticket.getId(),
                            ticket.getTenantId(),
                            ticket.getTitle(),
                            ticket.getDescription(),
                            ticket.getStatus(),
                            ticket.getAssigneeId(),
                            usernameOf(usernamesById, ticket.getAssigneeId()),
                            ticket.getCreatedBy(),
                            usernameOf(usernamesById, ticket.getCreatedBy()),
                            ticket.getCreatedAt(),
                            ticket.getUpdatedAt(),
                            comments.stream()
                                    .map(comment -> new TicketCommentView(
                                            comment.getId(),
                                            comment.getTicketId(),
                                            comment.getContent(),
                                            comment.getCreatedBy(),
                                            usernameOf(usernamesById, comment.getCreatedBy()),
                                            comment.getCreatedAt()
                                    ))
                                    .toList(),
                            operationLogs.stream()
                                    .map(log -> new TicketOperationLogView(
                                            log.getId(),
                                            log.getOperationType(),
                                            log.getDetail(),
                                            log.getOperatorId(),
                                            usernameOf(usernamesById, log.getOperatorId()),
                                            log.getCreatedAt()
                                    ))
                                    .toList()
                    );
                });
    }

    @Override
    public Optional<TicketPromptContext> findTicketPromptContext(Long tenantId, Long ticketId) {
        return ticketRepository.findByIdAndTenantId(ticketId, tenantId)
                .map(ticket -> {
                    // Prompt context intentionally uses a bounded recent window instead of full
                    // history so providers get the latest signal without unbounded token growth.
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

                    return new TicketPromptContext(
                            ticket.getId(),
                            ticket.getTenantId(),
                            ticket.getTitle(),
                            ticket.getDescription(),
                            ticket.getStatus(),
                            usernameOf(usernamesById, ticket.getAssigneeId()),
                            usernameOf(usernamesById, ticket.getCreatedBy()),
                            ticket.getCreatedAt(),
                            ticket.getUpdatedAt(),
                            comments.items().stream()
                                    .map(comment -> new TicketPromptContext.Comment(
                                            comment.getId(),
                                            comment.getContent(),
                                            usernameOf(usernamesById, comment.getCreatedBy()),
                                            comment.getCreatedAt()
                                    ))
                                    .toList(),
                            comments.olderItemsOmitted(),
                            operationLogs.items().stream()
                                    .map(log -> new TicketPromptContext.OperationLog(
                                            log.getId(),
                                            log.getOperationType(),
                                            log.getDetail(),
                                            usernameOf(usernamesById, log.getOperatorId()),
                                            log.getCreatedAt()
                                    ))
                                    .toList(),
                            operationLogs.olderItemsOmitted()
                    );
                });
    }

    private Map<Long, String> loadUsernamesById(Long tenantId, Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userRepository.findAllByTenantIdAndIdIn(tenantId, userIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, UserEntity::getUsername, (left, right) -> left));
    }

    private TicketListItem toListItem(TicketEntity ticket, Map<Long, String> usernamesById) {
        return new TicketListItem(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getStatus(),
                ticket.getAssigneeId(),
                usernameOf(usernamesById, ticket.getAssigneeId()),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
    }

    private String usernameOf(Map<Long, String> usernamesById, Long userId) {
        if (userId == null) {
            return null;
        }
        return usernamesById.get(userId);
    }

    private TicketAiInteractionItem toAiInteractionItem(AiInteractionRecordEntity record) {
        return new TicketAiInteractionItem(
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

    private WindowedComments loadRecentComments(Long ticketId, Long tenantId) {
        List<TicketCommentEntity> latestComments = ticketCommentRepository.findByTicketIdAndTenantIdOrderByIdDesc(
                ticketId,
                tenantId,
                PageRequest.of(0, TicketPromptWindowPolicy.COMMENT_HISTORY_LIMIT + 1)
        );
        // Fetch one extra row so the prompt can signal truncated history without having to load
        // the full comment stream first.
        boolean olderItemsOmitted = latestComments.size() > TicketPromptWindowPolicy.COMMENT_HISTORY_LIMIT;
        List<TicketCommentEntity> window = new ArrayList<>(
                latestComments.subList(0, Math.min(latestComments.size(), TicketPromptWindowPolicy.COMMENT_HISTORY_LIMIT))
        );
        window.sort(Comparator.comparing(TicketCommentEntity::getId));
        return new WindowedComments(List.copyOf(window), olderItemsOmitted);
    }

    private WindowedOperationLogs loadRecentOperationLogs(Long ticketId, Long tenantId) {
        List<TicketOperationLogEntity> latestOperationLogs = ticketOperationLogRepository.findByTicketIdAndTenantIdOrderByIdDesc(
                ticketId,
                tenantId,
                PageRequest.of(0, TicketPromptWindowPolicy.OPERATION_LOG_HISTORY_LIMIT + 1)
        );
        // The same extra-row pattern keeps prompt-window truncation explicit for operation logs
        // without paying for a second count-style query.
        boolean olderItemsOmitted = latestOperationLogs.size() > TicketPromptWindowPolicy.OPERATION_LOG_HISTORY_LIMIT;
        List<TicketOperationLogEntity> window = new ArrayList<>(
                latestOperationLogs.subList(0, Math.min(latestOperationLogs.size(), TicketPromptWindowPolicy.OPERATION_LOG_HISTORY_LIMIT))
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
