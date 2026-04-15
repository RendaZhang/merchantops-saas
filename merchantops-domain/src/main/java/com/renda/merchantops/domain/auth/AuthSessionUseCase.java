package com.renda.merchantops.domain.auth;

import java.time.LocalDateTime;

public interface AuthSessionUseCase {

    AuthSession createSession(Long tenantId,
                              Long userId,
                              LocalDateTime createdAt,
                              LocalDateTime expiresAt);

    boolean isSessionActive(String sessionId,
                            Long tenantId,
                            Long userId,
                            LocalDateTime now);

    void revokeSession(String sessionId,
                       Long tenantId,
                       Long userId,
                       LocalDateTime revokedAt);
}
