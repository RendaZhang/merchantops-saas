package com.renda.merchantops.domain.auth;

import java.time.LocalDateTime;
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
                                     LocalDateTime createdAt,
                                     LocalDateTime expiresAt) {
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
                                   LocalDateTime now) {
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
                              LocalDateTime revokedAt) {
        authSessionPort.findBySessionId(sessionId)
                .filter(session -> Objects.equals(session.tenantId(), tenantId))
                .filter(session -> Objects.equals(session.userId(), userId))
                .filter(session -> session.status() == AuthSessionStatus.ACTIVE)
                .filter(session -> session.revokedAt() == null)
                .ifPresent(session -> authSessionPort.revoke(session.sessionId(), revokedAt));
    }

    @Override
    public int cleanupExpiredOrRevokedSessions(LocalDateTime now,
                                               long retentionSeconds,
                                               int batchSize) {
        return authSessionPort.cleanupExpiredOrRevokedSessions(
                now.minusSeconds(retentionSeconds),
                batchSize
        );
    }
}
