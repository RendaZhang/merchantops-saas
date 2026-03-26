package com.renda.merchantops.domain.user;

import java.time.LocalDateTime;

public record UserWriteResult(
        Long id,
        Long tenantId,
        String username,
        String displayName,
        String email,
        String status,
        LocalDateTime updatedAt
) {
}
