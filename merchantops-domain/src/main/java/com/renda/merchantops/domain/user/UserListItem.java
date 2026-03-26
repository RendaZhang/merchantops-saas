package com.renda.merchantops.domain.user;

public record UserListItem(
        Long id,
        String username,
        String displayName,
        String email,
        String status
) {
}
