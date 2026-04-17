package com.renda.merchantops.infra.repository;

import com.renda.merchantops.infra.persistence.entity.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface RoleRepository extends JpaRepository<RoleEntity, Long> {

    @Query(value = """
            SELECT r.*
            FROM `role` r
            WHERE r.tenant_id = :tenantId
            ORDER BY r.id
            """, nativeQuery = true)
    List<RoleEntity> findAllByTenantIdOrderByIdAsc(@Param("tenantId") Long tenantId);

    @Query(value = """
            SELECT r.*
            FROM `role` r
            WHERE r.tenant_id = :tenantId
              AND r.role_code IN (:roleCodes)
            ORDER BY r.id
            """, nativeQuery = true)
    List<RoleEntity> findAllByTenantIdAndRoleCodeInOrderByIdAsc(@Param("tenantId") Long tenantId,
                                                                 @Param("roleCodes") Collection<String> roleCodes);

    @Query(value = """
            SELECT r.*
            FROM `role` r
            JOIN user_role ur ON ur.role_id = r.id AND ur.tenant_id = r.tenant_id
            WHERE ur.user_id = :userId
              AND ur.tenant_id = :tenantId
            ORDER BY r.id
            """, nativeQuery = true)
    List<RoleEntity> findRolesByUserIdAndTenantId(@Param("userId") Long userId,
                                                   @Param("tenantId") Long tenantId);

}
