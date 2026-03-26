package com.renda.merchantops.infra.ticket;

import com.renda.merchantops.domain.ticket.ManagedTicket;
import com.renda.merchantops.domain.ticket.NewTicketCommentDraft;
import com.renda.merchantops.domain.ticket.NewTicketDraft;
import com.renda.merchantops.domain.ticket.NewTicketOperationLogDraft;
import com.renda.merchantops.domain.ticket.StoredTicketComment;
import com.renda.merchantops.domain.ticket.TicketCommandPort;
import com.renda.merchantops.domain.ticket.TicketUpdateDraft;
import com.renda.merchantops.domain.ticket.TicketUserAccount;
import com.renda.merchantops.infra.persistence.entity.TicketCommentEntity;
import com.renda.merchantops.infra.persistence.entity.TicketEntity;
import com.renda.merchantops.infra.persistence.entity.TicketOperationLogEntity;
import com.renda.merchantops.infra.repository.TicketCommentRepository;
import com.renda.merchantops.infra.repository.TicketOperationLogRepository;
import com.renda.merchantops.infra.repository.TicketRepository;
import com.renda.merchantops.infra.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class JpaTicketCommandAdapter implements TicketCommandPort {

    private final TicketRepository ticketRepository;
    private final TicketCommentRepository ticketCommentRepository;
    private final TicketOperationLogRepository ticketOperationLogRepository;
    private final UserRepository userRepository;

    public JpaTicketCommandAdapter(TicketRepository ticketRepository,
                                   TicketCommentRepository ticketCommentRepository,
                                   TicketOperationLogRepository ticketOperationLogRepository,
                                   UserRepository userRepository) {
        this.ticketRepository = ticketRepository;
        this.ticketCommentRepository = ticketCommentRepository;
        this.ticketOperationLogRepository = ticketOperationLogRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Optional<ManagedTicket> findManagedTicket(Long tenantId, Long ticketId) {
        return ticketRepository.findByIdAndTenantId(ticketId, tenantId).map(this::toManagedTicket);
    }

    @Override
    public Optional<TicketUserAccount> findTenantUser(Long tenantId, Long userId) {
        return userRepository.findByIdAndTenantId(userId, tenantId)
                .map(user -> new TicketUserAccount(user.getId(), user.getUsername(), user.getStatus()));
    }

    @Override
    public ManagedTicket createTicket(NewTicketDraft draft) {
        TicketEntity entity = new TicketEntity();
        entity.setTenantId(draft.tenantId());
        entity.setTitle(draft.title());
        entity.setDescription(draft.description());
        entity.setStatus(draft.status());
        entity.setAssigneeId(null);
        entity.setCreatedBy(draft.createdBy());
        entity.setRequestId(draft.requestId());
        entity.setCreatedAt(draft.createdAt());
        entity.setUpdatedAt(draft.updatedAt());
        return toManagedTicket(ticketRepository.save(entity));
    }

    @Override
    public ManagedTicket saveTicket(TicketUpdateDraft draft) {
        TicketEntity entity = new TicketEntity();
        entity.setId(draft.id());
        entity.setTenantId(draft.tenantId());
        entity.setTitle(draft.title());
        entity.setDescription(draft.description());
        entity.setStatus(draft.status());
        entity.setAssigneeId(draft.assigneeId());
        entity.setCreatedBy(draft.createdBy());
        entity.setRequestId(draft.requestId());
        entity.setCreatedAt(draft.createdAt());
        entity.setUpdatedAt(draft.updatedAt());
        return toManagedTicket(ticketRepository.save(entity));
    }

    @Override
    public StoredTicketComment createComment(NewTicketCommentDraft draft) {
        TicketCommentEntity entity = new TicketCommentEntity();
        entity.setTenantId(draft.tenantId());
        entity.setTicketId(draft.ticketId());
        entity.setContent(draft.content());
        entity.setCreatedBy(draft.createdBy());
        entity.setRequestId(draft.requestId());
        entity.setCreatedAt(draft.createdAt());
        TicketCommentEntity saved = ticketCommentRepository.save(entity);
        return new StoredTicketComment(
                saved.getId(),
                saved.getTicketId(),
                saved.getContent(),
                saved.getCreatedBy(),
                saved.getCreatedAt()
        );
    }

    @Override
    public void appendOperationLog(NewTicketOperationLogDraft draft) {
        TicketOperationLogEntity entity = new TicketOperationLogEntity();
        entity.setTicketId(draft.ticketId());
        entity.setTenantId(draft.tenantId());
        entity.setOperatorId(draft.operatorId());
        entity.setRequestId(draft.requestId());
        entity.setOperationType(draft.operationType());
        entity.setDetail(draft.detail());
        entity.setCreatedAt(draft.createdAt());
        ticketOperationLogRepository.save(entity);
    }

    private ManagedTicket toManagedTicket(TicketEntity ticket) {
        return new ManagedTicket(
                ticket.getId(),
                ticket.getTenantId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getStatus(),
                ticket.getAssigneeId(),
                ticket.getCreatedBy(),
                ticket.getRequestId(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
    }
}
