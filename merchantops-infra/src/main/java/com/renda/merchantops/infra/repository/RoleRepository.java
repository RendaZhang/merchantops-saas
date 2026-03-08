package com.renda.merchantops.infra.repository;

import com.renda.merchantops.infra.persistence.entity.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RoleRepository extends JpaRepository<RoleEntity, Long> {

    @Query(value = """
            SELECT r.*
            FROM `role` r
            JOIN user_role ur ON ur.role_id = r.id
            WHERE ur.user_id = :userId
              AND r.tenant_id = :tenantId
            ORDER BY r.id
            """, nativeQuery = true)
    List<RoleEntity> findRolesByUserIdAndTenantId(@Param("userId") Long userId,
                                                   @Param("tenantId") Long tenantId);

}
