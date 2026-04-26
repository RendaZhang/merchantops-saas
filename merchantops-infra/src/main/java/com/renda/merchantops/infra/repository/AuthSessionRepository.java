package com.renda.merchantops.infra.repository;

import com.renda.merchantops.infra.persistence.entity.AuthSessionEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AuthSessionRepository extends JpaRepository<AuthSessionEntity, Long> {

    Optional<AuthSessionEntity> findBySessionId(String sessionId);

    @Query("""
            select a.id
            from AuthSessionEntity a
            where (a.status = :activeStatus and a.expiresAt < :cutoff)
               or (a.status = :revokedStatus and a.revokedAt < :cutoff)
            order by a.id asc
            """)
    List<Long> findCleanupCandidateIds(@Param("activeStatus") String activeStatus,
                                       @Param("revokedStatus") String revokedStatus,
                                       @Param("cutoff") LocalDateTime cutoff,
                                       Pageable pageable);

    @Modifying
    @Query("delete from AuthSessionEntity a where a.id in :ids")
    int deleteByIdIn(@Param("ids") List<Long> ids);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update AuthSessionEntity a
            set a.status = :revokedStatus,
                a.revokedAt = :revokedAt
            where a.tenantId = :tenantId
              and a.userId = :userId
              and a.status = :activeStatus
            """)
    int revokeActiveSessionsForUser(@Param("tenantId") Long tenantId,
                                    @Param("userId") Long userId,
                                    @Param("activeStatus") String activeStatus,
                                    @Param("revokedStatus") String revokedStatus,
                                    @Param("revokedAt") LocalDateTime revokedAt);
}
