package com.renda.merchantops.infra.repository;

import com.renda.merchantops.infra.persistence.entity.AuthSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthSessionRepository extends JpaRepository<AuthSessionEntity, Long> {

    Optional<AuthSessionEntity> findBySessionId(String sessionId);
}
