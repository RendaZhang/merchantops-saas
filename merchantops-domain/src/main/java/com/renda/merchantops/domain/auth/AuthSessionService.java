package com.renda.merchantops.domain.auth;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class AuthSessionService implements AuthSessionUseCase {

    private final AuthSessionPort authSessionPort;

    public AuthSessionService(AuthSessionPort authSessionPort) {
        this.authSessionPort = authSessionPort;
    }

    @Override
    public AuthSession createSession(Long tenantId,
                                     Long userId,
                                     Instant createdAt,
                                     Instant expiresAt) {
        return authSessionPort.create(
                UUID.randomUUID().toString(),
                tenantId,
                userId,
                createdAt,
                expiresAt
        );
    }

    @Override
    public boolean isSessionActive(String sessionId,
                                   Long tenantId,
                                   Long userId,
                                   Instant now) {
        return authSessionPort.findBySessionId(sessionId)
                .filter(session -> Objects.equals(session.tenantId(), tenantId))
                .filter(session -> Objects.equals(session.userId(), userId))
                .filter(session -> session.status() == AuthSessionStatus.ACTIVE)
                .filter(session -> session.revokedAt() == null)
                .filter(session -> session.expiresAt().isAfter(now))
                .isPresent();
    }

    @Override
    public void revokeSession(String sessionId,
                              Long tenantId,
                              Long userId,
                              Instant revokedAt) {
        authSessionPort.findBySessionId(sessionId)
                .filter(session -> Objects.equals(session.tenantId(), tenantId))
                .filter(session -> Objects.equals(session.userId(), userId))
                .filter(session -> session.status() == AuthSessionStatus.ACTIVE)
                .filter(session -> session.revokedAt() == null)
                .ifPresent(session -> authSessionPort.revoke(session.sessionId(), revokedAt));
    }

    @Override
    public int revokeAllSessions(Long tenantId,
                                 Long userId,
                                 Instant revokedAt) {
        return authSessionPort.revokeActiveSessionsForUser(tenantId, userId, revokedAt);
    }

    @Override
    public int cleanupExpiredOrRevokedSessions(Instant now,
                                               long retentionSeconds,
                                               int batchSize) {
        return authSessionPort.cleanupExpiredOrRevokedSessions(
                now.minusSeconds(retentionSeconds),
                batchSize
        );
    }
}
