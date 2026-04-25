package com.renda.merchantops.domain.auth;

import java.time.Instant;

public record AuthSession(
        Long id,
        String sessionId,
        Long tenantId,
        Long userId,
        AuthSessionStatus status,
        Instant createdAt,
        Instant expiresAt,
        Instant revokedAt
) {
}
