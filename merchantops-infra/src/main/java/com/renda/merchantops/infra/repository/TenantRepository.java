package com.renda.merchantops.infra.repository;

import com.renda.merchantops.infra.persistence.entity.TenantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TenantRepository extends JpaRepository<TenantEntity, Long> {

    Optional<TenantEntity> findByTenantCode(String tenantCode);

}
