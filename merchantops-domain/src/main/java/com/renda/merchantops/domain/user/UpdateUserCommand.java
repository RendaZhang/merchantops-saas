package com.renda.merchantops.domain.user;

public record UpdateUserCommand(
        String displayName,
        String email
) {
}
