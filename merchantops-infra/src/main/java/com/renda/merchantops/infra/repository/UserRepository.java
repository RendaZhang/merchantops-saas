package com.renda.merchantops.infra.repository;

import com.renda.merchantops.infra.persistence.entity.UserEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByIdAndTenantId(Long id, Long tenantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UserEntity u where u.id = :id and u.tenantId = :tenantId")
    Optional<UserEntity> findByIdAndTenantIdForUpdate(@Param("id") Long id, @Param("tenantId") Long tenantId);

    Optional<UserEntity> findByTenantIdAndUsername(Long tenantId, String username);

    List<UserEntity> findAllByTenantIdAndIdIn(Long tenantId, Collection<Long> ids);

    boolean existsByTenantIdAndUsername(Long tenantId, String username);

    boolean existsByTenantIdAndUsernameAndIdNot(Long tenantId, String username, Long id);

    List<UserEntity> findAllByTenantIdOrderByIdAsc(Long tenantId);

    List<UserEntity> findAllByTenantIdAndStatusOrderByIdAsc(Long tenantId, String status);

    Page<UserEntity> findAllByTenantId(Long tenantId, Pageable pageable);

    Page<UserEntity> findAllByTenantIdAndStatus(Long tenantId, String status, Pageable pageable);

    @Query(value = """
            SELECT DISTINCT u.*
            FROM users u
            LEFT JOIN user_role ur ON ur.user_id = u.id
            LEFT JOIN `role` r ON r.id = ur.role_id AND r.tenant_id = u.tenant_id
            WHERE u.tenant_id = :tenantId
              AND (:username IS NULL OR u.username LIKE CONCAT('%', :username, '%'))
              AND (:status IS NULL OR u.status = :status)
              AND (:roleCode IS NULL OR r.role_code = :roleCode)
            ORDER BY u.id ASC
            """,
            countQuery = """
                    SELECT COUNT(DISTINCT u.id)
                    FROM users u
                    LEFT JOIN user_role ur ON ur.user_id = u.id
                    LEFT JOIN `role` r ON r.id = ur.role_id AND r.tenant_id = u.tenant_id
                    WHERE u.tenant_id = :tenantId
                      AND (:username IS NULL OR u.username LIKE CONCAT('%', :username, '%'))
                      AND (:status IS NULL OR u.status = :status)
                      AND (:roleCode IS NULL OR r.role_code = :roleCode)
                    """,
            nativeQuery = true)
    Page<UserEntity> searchPageByTenantId(@Param("tenantId") Long tenantId,
                                          @Param("username") String username,
                                          @Param("status") String status,
                                          @Param("roleCode") String roleCode,
                                          Pageable pageable);

}
