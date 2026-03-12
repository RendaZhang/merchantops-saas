package com.renda.merchantops.infra.repository;

import com.renda.merchantops.infra.persistence.entity.ImportJobItemErrorEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ImportJobItemErrorRepository extends JpaRepository<ImportJobItemErrorEntity, Long> {

    @Query("""
            select e
            from ImportJobItemErrorEntity e
            where e.tenantId = :tenantId
              and e.importJobId = :importJobId
            order by case when e.rowNumber is null then 0 else 1 end asc, e.rowNumber asc, e.id asc
            """)
    List<ImportJobItemErrorEntity> findAllByTenantIdAndImportJobIdOrderByRowNumberAscIdAsc(@Param("tenantId") Long tenantId,
                                                                                             @Param("importJobId") Long importJobId);

    @Query("""
            select e
            from ImportJobItemErrorEntity e
            where e.tenantId = :tenantId
              and e.importJobId = :importJobId
              and e.rowNumber is not null
              and e.rowNumber > 1
            order by e.rowNumber asc, e.id asc
            """)
    List<ImportJobItemErrorEntity> findReplayableRowsByTenantIdAndImportJobId(@Param("tenantId") Long tenantId,
                                                                               @Param("importJobId") Long importJobId);

    @Query(
            value = """
                    select e
                    from ImportJobItemErrorEntity e
                    where e.tenantId = :tenantId
                      and e.importJobId = :importJobId
                      and (:errorCode is null or e.errorCode = :errorCode)
                    order by case when e.rowNumber is null then 0 else 1 end asc, e.rowNumber asc, e.id asc
                    """,
            countQuery = """
                    select count(e)
                    from ImportJobItemErrorEntity e
                    where e.tenantId = :tenantId
                      and e.importJobId = :importJobId
                      and (:errorCode is null or e.errorCode = :errorCode)
                    """
    )
    Page<ImportJobItemErrorEntity> searchPageByTenantIdAndImportJobId(@Param("tenantId") Long tenantId,
                                                                      @Param("importJobId") Long importJobId,
                                                                      @Param("errorCode") String errorCode,
                                                                      Pageable pageable);

    @Query("""
            select e.errorCode as errorCode, count(e) as errorCount
            from ImportJobItemErrorEntity e
            where e.tenantId = :tenantId
              and e.importJobId = :importJobId
            group by e.errorCode
            order by count(e) desc, e.errorCode asc
            """)
    List<ImportJobErrorCodeCountView> summarizeErrorCodesByTenantIdAndImportJobId(@Param("tenantId") Long tenantId,
                                                                                   @Param("importJobId") Long importJobId);

    interface ImportJobErrorCodeCountView {

        String getErrorCode();

        long getErrorCount();
    }
}
