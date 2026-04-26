package com.renda.merchantops.domain.auth;

import java.time.Instant;

public interface AuthSessionUseCase {

    AuthSession createSession(Long tenantId,
                              Long userId,
                              Instant createdAt,
                              Instant expiresAt);

    boolean isSessionActive(String sessionId,
                            Long tenantId,
                            Long userId,
                            Instant now);

    void revokeSession(String sessionId,
                       Long tenantId,
                       Long userId,
                       Instant revokedAt);

    int revokeAllSessions(Long tenantId,
                          Long userId,
                          Instant revokedAt);

    int cleanupExpiredOrRevokedSessions(Instant now,
                                        long retentionSeconds,
                                        int batchSize);
}
