package com.renda.merchantops.domain.auth;

import java.time.Instant;
import java.util.List;

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

    List<AuthSession> listSessionsForUser(Long tenantId,
                                          Long userId);

    int cleanupExpiredOrRevokedSessions(Instant now,
                                        long retentionSeconds,
                                        int batchSize);
}
