package com.renda.merchantops.domain.auth;

import java.time.LocalDateTime;
import java.util.Optional;

public interface AuthSessionPort {

    AuthSession create(String sessionId,
                       Long tenantId,
                       Long userId,
                       LocalDateTime createdAt,
                       LocalDateTime expiresAt);

    Optional<AuthSession> findBySessionId(String sessionId);

    void revoke(String sessionId, LocalDateTime revokedAt);

    int revokeActiveSessionsForUser(Long tenantId, Long userId, LocalDateTime revokedAt);

    int cleanupExpiredOrRevokedSessions(LocalDateTime cutoff, int limit);
}
