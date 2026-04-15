package com.renda.merchantops.domain.auth;

import java.time.LocalDateTime;

public record AuthSession(
        Long id,
        String sessionId,
        Long tenantId,
        Long userId,
        AuthSessionStatus status,
        LocalDateTime createdAt,
        LocalDateTime expiresAt,
        LocalDateTime revokedAt
) {
}
