package com.renda.merchantops.api.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "One current-user auth session")
public record AuthSessionListItemResponse(
        @Schema(description = "Whether this row is the session identified by the caller's current JWT sid", example = "true")
        boolean currentSession,
        @Schema(description = "Computed session status: ACTIVE, EXPIRED, or REVOKED", example = "ACTIVE")
        String status,
        @Schema(description = "Session creation time as a UTC instant", example = "2026-05-19T10:00:00Z")
        Instant createdAt,
        @Schema(description = "Session expiry time as a UTC instant", example = "2026-05-19T12:00:00Z")
        Instant expiresAt,
        @Schema(description = "Session revocation time as a UTC instant, null when not revoked", example = "2026-05-19T11:30:00Z")
        Instant revokedAt
) {
}
