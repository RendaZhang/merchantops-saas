package com.renda.merchantops.domain.auth;

public interface AccessValidationUseCase {

    AccessValidationResult validate(AccessPrincipal tokenUser);
}
