package com.renda.merchantops.domain.user;

import java.util.List;

public record CreateUserCommand(
        String username,
        String password,
        String displayName,
        String email,
        List<String> roleCodes
) {
}
