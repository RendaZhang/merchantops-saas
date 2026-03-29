package com.renda.merchantops.infra.repository;

import com.renda.merchantops.infra.persistence.entity.AiInteractionRecordEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AiInteractionRecordRepository extends JpaRepository<AiInteractionRecordEntity, Long> {

    @Query("""
            select a from AiInteractionRecordEntity a
            where a.tenantId = :tenantId
              and a.entityType = :entityType
              and a.entityId = :entityId
              and (:interactionType is null or a.interactionType = :interactionType)
              and (:status is null or a.status = :status)
            """)
    Page<AiInteractionRecordEntity> searchPageByTenantIdAndEntity(@Param("tenantId") Long tenantId,
                                                                  @Param("entityType") String entityType,
                                                                  @Param("entityId") Long entityId,
                                                                  @Param("interactionType") String interactionType,
                                                                  @Param("status") String status,
                                                                  Pageable pageable);

    Optional<AiInteractionRecordEntity> findByIdAndTenantIdAndEntityTypeAndEntityId(Long id,
                                                                                     Long tenantId,
                                                                                     String entityType,
                                                                                     Long entityId);
}
