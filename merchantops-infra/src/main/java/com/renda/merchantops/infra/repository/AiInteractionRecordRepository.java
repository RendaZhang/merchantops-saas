package com.renda.merchantops.infra.repository;

import com.renda.merchantops.infra.persistence.entity.AiInteractionRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiInteractionRecordRepository extends JpaRepository<AiInteractionRecordEntity, Long> {
}
