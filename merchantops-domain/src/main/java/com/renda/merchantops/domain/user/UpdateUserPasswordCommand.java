package com.renda.merchantops.domain.user;

public record UpdateUserPasswordCommand(
        String passwordHash
) {
}
