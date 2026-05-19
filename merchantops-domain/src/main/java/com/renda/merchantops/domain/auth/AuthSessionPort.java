package com.renda.merchantops.domain.auth;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AuthSessionPort {

    AuthSession create(String sessionId,
                       Long tenantId,
                       Long userId,
                       Instant createdAt,
                       Instant expiresAt);

    Optional<AuthSession> findBySessionId(String sessionId);

    void revoke(String sessionId, Instant revokedAt);

    int revokeActiveSessionsForUser(Long tenantId, Long userId, Instant revokedAt);

    List<AuthSession> findAllByTenantIdAndUserId(Long tenantId, Long userId);

    int cleanupExpiredOrRevokedSessions(Instant cutoff, int limit);
}
