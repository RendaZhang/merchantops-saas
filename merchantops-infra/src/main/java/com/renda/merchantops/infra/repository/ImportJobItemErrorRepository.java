package com.renda.merchantops.infra.repository;

import com.renda.merchantops.infra.persistence.entity.ImportJobItemErrorEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImportJobItemErrorRepository extends JpaRepository<ImportJobItemErrorEntity, Long> {

    List<ImportJobItemErrorEntity> findAllByTenantIdAndImportJobIdOrderByIdAsc(Long tenantId, Long importJobId);
}
