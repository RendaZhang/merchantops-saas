package com.renda.merchantops.infra.repository;

import com.renda.merchantops.infra.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByTenantIdAndUsername(Long tenantId, String username);

}
