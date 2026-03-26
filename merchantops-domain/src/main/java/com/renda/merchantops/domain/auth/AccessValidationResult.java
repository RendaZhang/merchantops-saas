package com.renda.merchantops.domain.auth;

public record AccessValidationResult(
        AccessValidationStatus status,
        ValidatedAccessPrincipal currentUser
) {
}
