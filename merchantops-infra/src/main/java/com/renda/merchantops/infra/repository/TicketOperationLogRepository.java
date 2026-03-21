package com.renda.merchantops.infra.repository;

import com.renda.merchantops.infra.persistence.entity.TicketOperationLogEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketOperationLogRepository extends JpaRepository<TicketOperationLogEntity, Long> {

    List<TicketOperationLogEntity> findAllByTicketIdAndTenantIdOrderByIdAsc(Long ticketId, Long tenantId);

    List<TicketOperationLogEntity> findByTicketIdAndTenantIdOrderByIdDesc(Long ticketId, Long tenantId, Pageable pageable);
}
