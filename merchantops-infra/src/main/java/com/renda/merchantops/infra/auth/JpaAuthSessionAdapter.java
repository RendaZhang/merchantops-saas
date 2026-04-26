package com.renda.merchantops.infra.auth;

import com.renda.merchantops.domain.auth.AuthSession;
import com.renda.merchantops.domain.auth.AuthSessionPort;
import com.renda.merchantops.domain.auth.AuthSessionStatus;
import com.renda.merchantops.infra.persistence.entity.AuthSessionEntity;
import com.renda.merchantops.infra.repository.AuthSessionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Component
public class JpaAuthSessionAdapter implements AuthSessionPort {

    private final AuthSessionRepository authSessionRepository;

    public JpaAuthSessionAdapter(AuthSessionRepository authSessionRepository) {
        this.authSessionRepository = authSessionRepository;
    }

    @Override
    @Transactional
    public AuthSession create(String sessionId,
                              Long tenantId,
                              Long userId,
                              Instant createdAt,
                              Instant expiresAt) {
        AuthSessionEntity entity = new AuthSessionEntity();
        entity.setSessionId(sessionId);
        entity.setTenantId(tenantId);
        entity.setUserId(userId);
        entity.setStatus(AuthSessionStatus.ACTIVE.name());
        entity.setCreatedAt(toUtcLocalDateTime(createdAt));
        entity.setExpiresAt(toUtcLocalDateTime(expiresAt));

        return toDomain(authSessionRepository.save(entity));
    }

    @Override
    public Optional<AuthSession> findBySessionId(String sessionId) {
        return authSessionRepository.findBySessionId(sessionId).map(this::toDomain);
    }

    @Override
    @Transactional
    public void revoke(String sessionId, Instant revokedAt) {
        authSessionRepository.findBySessionId(sessionId).ifPresent(entity -> {
            entity.setStatus(AuthSessionStatus.REVOKED.name());
            entity.setRevokedAt(toUtcLocalDateTime(revokedAt));
            authSessionRepository.save(entity);
        });
    }

    @Override
    @Transactional
    public int revokeActiveSessionsForUser(Long tenantId, Long userId, Instant revokedAt) {
        return authSessionRepository.revokeActiveSessionsForUser(
                tenantId,
                userId,
                AuthSessionStatus.ACTIVE.name(),
                AuthSessionStatus.REVOKED.name(),
                toUtcLocalDateTime(revokedAt)
        );
    }

    @Override
    @Transactional
    public int cleanupExpiredOrRevokedSessions(Instant cutoff, int limit) {
        List<Long> candidateIds = authSessionRepository.findCleanupCandidateIds(
                AuthSessionStatus.ACTIVE.name(),
                AuthSessionStatus.REVOKED.name(),
                toUtcLocalDateTime(cutoff),
                PageRequest.of(0, limit)
        );
        if (candidateIds.isEmpty()) {
            return 0;
        }
        return authSessionRepository.deleteByIdIn(candidateIds);
    }

    private AuthSession toDomain(AuthSessionEntity entity) {
        return new AuthSession(
                entity.getId(),
                entity.getSessionId(),
                entity.getTenantId(),
                entity.getUserId(),
                AuthSessionStatus.valueOf(entity.getStatus()),
                toUtcInstant(entity.getCreatedAt()),
                toUtcInstant(entity.getExpiresAt()),
                toUtcInstant(entity.getRevokedAt())
        );
    }

    private LocalDateTime toUtcLocalDateTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private Instant toUtcInstant(LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return value.toInstant(ZoneOffset.UTC);
    }
}
