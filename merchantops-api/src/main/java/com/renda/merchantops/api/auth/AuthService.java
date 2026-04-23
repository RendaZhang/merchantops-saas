package com.renda.merchantops.api.auth;

import com.renda.merchantops.api.dto.auth.LoginRequest;
import com.renda.merchantops.api.dto.auth.LoginResponse;
import com.renda.merchantops.api.security.CurrentUser;
import com.renda.merchantops.api.security.JwtTokenService;
import com.renda.merchantops.domain.auth.AuthSession;
import com.renda.merchantops.domain.auth.AuthSessionUseCase;
import com.renda.merchantops.domain.auth.AuthenticationCommand;
import com.renda.merchantops.domain.auth.AuthenticationResult;
import com.renda.merchantops.domain.auth.AuthenticationUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationUseCase authenticationUseCase;
    private final AuthSessionUseCase authSessionUseCase;
    private final JwtTokenService jwtTokenService;

    public LoginResponse login(LoginRequest request) {
        AuthenticationResult authentication = authenticationUseCase.authenticate(
                new AuthenticationCommand(
                        request.getTenantCode(),
                        request.getUsername(),
                        request.getPassword()
                )
        );

        LocalDateTime issuedAt = LocalDateTime.now();
        LocalDateTime expiresAt = issuedAt.plusSeconds(jwtTokenService.getExpireSeconds());
        AuthSession authSession = authSessionUseCase.createSession(
                authentication.tenantId(),
                authentication.userId(),
                issuedAt,
                expiresAt
        );

        String token = jwtTokenService.generateToken(
                authentication.userId(),
                authentication.tenantId(),
                authentication.tenantCode(),
                authentication.username(),
                authentication.roles(),
                authentication.permissions(),
                authSession.sessionId(),
                issuedAt,
                expiresAt
        );

        return new LoginResponse(token, "Bearer", jwtTokenService.getExpireSeconds());
    }

    public void logout(CurrentUser currentUser) {
        authSessionUseCase.revokeSession(
                currentUser.getSessionId(),
                currentUser.getTenantId(),
                currentUser.getUserId(),
                LocalDateTime.now()
        );
    }

    public int logoutAll(CurrentUser currentUser) {
        return authSessionUseCase.revokeAllSessions(
                currentUser.getTenantId(),
                currentUser.getUserId(),
                LocalDateTime.now()
        );
    }
}
