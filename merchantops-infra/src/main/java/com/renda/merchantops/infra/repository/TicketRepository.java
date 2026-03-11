package com.renda.merchantops.infra.repository;

import com.renda.merchantops.infra.persistence.entity.TicketEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TicketRepository extends JpaRepository<TicketEntity, Long> {

    Optional<TicketEntity> findByIdAndTenantId(Long id, Long tenantId);

    @Query("""
            SELECT t
            FROM TicketEntity t
            WHERE t.tenantId = :tenantId
              AND (:status IS NULL OR t.status = :status)
              AND (:assigneeId IS NULL OR t.assigneeId = :assigneeId)
              AND (:unassignedOnly = false OR t.assigneeId IS NULL)
              AND (:keyword IS NULL OR lower(t.title) LIKE lower(concat('%', :keyword, '%')) OR lower(coalesce(t.description, '')) LIKE lower(concat('%', :keyword, '%')))
            """)
    Page<TicketEntity> pageByTenantAndFilters(@Param("tenantId") Long tenantId,
                                               @Param("status") String status,
                                               @Param("assigneeId") Long assigneeId,
                                               @Param("keyword") String keyword,
                                               @Param("unassignedOnly") boolean unassignedOnly,
                                               Pageable pageable);
}
