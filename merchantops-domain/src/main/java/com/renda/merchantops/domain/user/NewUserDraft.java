package com.renda.merchantops.domain.user;

import java.time.LocalDateTime;

public record NewUserDraft(
        Long tenantId,
        String username,
        String passwordHash,
        String displayName,
        String email,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Long createdBy,
        Long updatedBy
) {
}
