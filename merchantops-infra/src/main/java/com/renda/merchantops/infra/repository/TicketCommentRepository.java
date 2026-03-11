package com.renda.merchantops.infra.repository;

import com.renda.merchantops.infra.persistence.entity.TicketCommentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketCommentRepository extends JpaRepository<TicketCommentEntity, Long> {

    List<TicketCommentEntity> findAllByTicketIdAndTenantIdOrderByIdAsc(Long ticketId, Long tenantId);
}
