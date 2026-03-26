package com.renda.merchantops.api.auth;

import com.renda.merchantops.api.dto.auth.LoginRequest;
import com.renda.merchantops.api.dto.auth.LoginResponse;
import com.renda.merchantops.api.security.JwtTokenService;
import com.renda.merchantops.domain.auth.AuthenticationCommand;
import com.renda.merchantops.domain.auth.AuthenticationResult;
import com.renda.merchantops.domain.auth.AuthenticationUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationUseCase authenticationUseCase;
    private final JwtTokenService jwtTokenService;

    public LoginResponse login(LoginRequest request) {
        AuthenticationResult authentication = authenticationUseCase.authenticate(
                new AuthenticationCommand(
                        request.getTenantCode(),
                        request.getUsername(),
                        request.getPassword()
                )
        );

        String token = jwtTokenService.generateToken(
                authentication.userId(),
                authentication.tenantId(),
                authentication.tenantCode(),
                authentication.username(),
                authentication.roles(),
                authentication.permissions()
        );

        return new LoginResponse(token, "Bearer", jwtTokenService.getExpireSeconds());
    }
}
