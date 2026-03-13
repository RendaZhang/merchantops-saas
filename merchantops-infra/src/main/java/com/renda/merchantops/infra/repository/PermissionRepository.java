package com.renda.merchantops.infra.repository;

import com.renda.merchantops.infra.persistence.entity.PermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PermissionRepository extends JpaRepository<PermissionEntity, Long> {

    @Query(value = """
            SELECT DISTINCT p.*
            FROM permission p
            JOIN role_permission rp ON rp.permission_id = p.id
            JOIN user_role ur ON ur.role_id = rp.role_id
            JOIN `role` r ON r.id = ur.role_id
            WHERE ur.user_id = :userId
              AND r.tenant_id = :tenantId
            ORDER BY p.id
            """, nativeQuery = true)
    List<PermissionEntity> findPermissionsByUserIdAndTenantId(@Param("userId") Long userId,
                                                               @Param("tenantId") Long tenantId);

}
