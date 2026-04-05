package com.renda.merchantops.infra.repository;

import com.renda.merchantops.infra.persistence.entity.AiInteractionRecordEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
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

    @Query("""
            select count(a) as totalInteractions,
                   coalesce(sum(case when a.status = 'SUCCEEDED' then 1 else 0 end), 0) as succeededCount,
                   coalesce(sum(case when a.status <> 'SUCCEEDED' then 1 else 0 end), 0) as failedCount,
                   coalesce(sum(a.usagePromptTokens), 0) as totalPromptTokens,
                   coalesce(sum(a.usageCompletionTokens), 0) as totalCompletionTokens,
                   coalesce(sum(a.usageTotalTokens), 0) as totalTokens,
                   coalesce(sum(a.usageCostMicros), 0) as totalCostMicros
            from AiInteractionRecordEntity a
            where a.tenantId = :tenantId
              and (:from is null or a.createdAt >= :from)
              and (:to is null or a.createdAt <= :to)
              and (:entityType is null or a.entityType = :entityType)
              and (:interactionType is null or a.interactionType = :interactionType)
              and (:status is null or a.status = :status)
            """)
    AiInteractionUsageSummaryTotalsView summarizeUsageByTenant(@Param("tenantId") Long tenantId,
                                                               @Param("from") LocalDateTime from,
                                                               @Param("to") LocalDateTime to,
                                                               @Param("entityType") String entityType,
                                                               @Param("interactionType") String interactionType,
                                                               @Param("status") String status);

    @Query("""
            select a.interactionType as interactionType,
                   count(a) as interactionCount,
                   coalesce(sum(case when a.status = 'SUCCEEDED' then 1 else 0 end), 0) as succeededCount,
                   coalesce(sum(case when a.status <> 'SUCCEEDED' then 1 else 0 end), 0) as failedCount,
                   coalesce(sum(a.usageTotalTokens), 0) as totalTokens,
                   coalesce(sum(a.usageCostMicros), 0) as totalCostMicros
            from AiInteractionRecordEntity a
            where a.tenantId = :tenantId
              and (:from is null or a.createdAt >= :from)
              and (:to is null or a.createdAt <= :to)
              and (:entityType is null or a.entityType = :entityType)
              and (:interactionType is null or a.interactionType = :interactionType)
              and (:status is null or a.status = :status)
            group by a.interactionType
            order by count(a) desc, a.interactionType asc
            """)
    List<AiInteractionUsageByInteractionTypeView> summarizeUsageByInteractionType(@Param("tenantId") Long tenantId,
                                                                                  @Param("from") LocalDateTime from,
                                                                                  @Param("to") LocalDateTime to,
                                                                                  @Param("entityType") String entityType,
                                                                                  @Param("interactionType") String interactionType,
                                                                                  @Param("status") String status);

    @Query("""
            select a.status as status,
                   count(a) as interactionCount,
                   coalesce(sum(a.usageTotalTokens), 0) as totalTokens,
                   coalesce(sum(a.usageCostMicros), 0) as totalCostMicros
            from AiInteractionRecordEntity a
            where a.tenantId = :tenantId
              and (:from is null or a.createdAt >= :from)
              and (:to is null or a.createdAt <= :to)
              and (:entityType is null or a.entityType = :entityType)
              and (:interactionType is null or a.interactionType = :interactionType)
              and (:status is null or a.status = :status)
            group by a.status
            order by count(a) desc, a.status asc
            """)
    List<AiInteractionUsageByStatusView> summarizeUsageByStatus(@Param("tenantId") Long tenantId,
                                                                @Param("from") LocalDateTime from,
                                                                @Param("to") LocalDateTime to,
                                                                @Param("entityType") String entityType,
                                                                @Param("interactionType") String interactionType,
                                                                @Param("status") String status);

    interface AiInteractionUsageSummaryTotalsView {

        Long getTotalInteractions();

        Long getSucceededCount();

        Long getFailedCount();

        Long getTotalPromptTokens();

        Long getTotalCompletionTokens();

        Long getTotalTokens();

        Long getTotalCostMicros();
    }

    interface AiInteractionUsageByInteractionTypeView {

        String getInteractionType();

        Long getInteractionCount();

        Long getSucceededCount();

        Long getFailedCount();

        Long getTotalTokens();

        Long getTotalCostMicros();
    }

    interface AiInteractionUsageByStatusView {

        String getStatus();

        Long getInteractionCount();

        Long getTotalTokens();

        Long getTotalCostMicros();
    }
}
