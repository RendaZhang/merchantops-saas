package com.renda.merchantops.domain.auth;

public interface AuthenticationUseCase {

    AuthenticationResult authenticate(AuthenticationCommand command);
}
