package com.renda.merchantops.infra.repository;

import com.renda.merchantops.infra.persistence.entity.TicketEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TicketRepository extends JpaRepository<TicketEntity, Long> {

    Optional<TicketEntity> findByIdAndTenantId(Long id, Long tenantId);

    Page<TicketEntity> findAllByTenantId(Long tenantId, Pageable pageable);

    Page<TicketEntity> findAllByTenantIdAndStatus(Long tenantId, String status, Pageable pageable);
}
