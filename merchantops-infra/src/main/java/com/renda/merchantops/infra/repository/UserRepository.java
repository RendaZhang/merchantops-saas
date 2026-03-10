package com.renda.merchantops.infra.repository;

import com.renda.merchantops.infra.persistence.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByIdAndTenantId(Long id, Long tenantId);

    Optional<UserEntity> findByTenantIdAndUsername(Long tenantId, String username);

    boolean existsByTenantIdAndUsername(Long tenantId, String username);

    boolean existsByTenantIdAndUsernameAndIdNot(Long tenantId, String username, Long id);

    List<UserEntity> findAllByTenantIdOrderByIdAsc(Long tenantId);

    List<UserEntity> findAllByTenantIdAndStatusOrderByIdAsc(Long tenantId, String status);

    Page<UserEntity> findAllByTenantId(Long tenantId, Pageable pageable);

    Page<UserEntity> findAllByTenantIdAndStatus(Long tenantId, String status, Pageable pageable);

}
