package com.renda.merchantops.api.auth;

import com.renda.merchantops.api.dto.auth.AuthSessionListItemResponse;
import com.renda.merchantops.api.dto.auth.AuthSessionListResponse;
import com.renda.merchantops.api.dto.auth.LoginRequest;
import com.renda.merchantops.api.dto.auth.LoginResponse;
import com.renda.merchantops.api.security.CurrentUser;
import com.renda.merchantops.api.security.JwtTokenService;
import com.renda.merchantops.domain.auth.AuthSession;
import com.renda.merchantops.domain.auth.AuthSessionStatus;
import com.renda.merchantops.domain.auth.AuthSessionUseCase;
import com.renda.merchantops.domain.auth.AuthenticationCommand;
import com.renda.merchantops.domain.auth.AuthenticationResult;
import com.renda.merchantops.domain.auth.AuthenticationUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

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

        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(jwtTokenService.getExpireSeconds());
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
                Instant.now()
        );
    }

    public int logoutAll(CurrentUser currentUser) {
        return authSessionUseCase.revokeAllSessions(
                currentUser.getTenantId(),
                currentUser.getUserId(),
                Instant.now()
        );
    }

    public AuthSessionListResponse listSessions(CurrentUser currentUser) {
        Instant now = Instant.now();
        List<AuthSessionListItemResponse> items = authSessionUseCase.listSessionsForUser(
                        currentUser.getTenantId(),
                        currentUser.getUserId()
                )
                .stream()
                .map(session -> new AuthSessionListItemResponse(
                        Objects.equals(session.sessionId(), currentUser.getSessionId()),
                        resolveSessionStatus(session, now),
                        session.createdAt(),
                        session.expiresAt(),
                        session.revokedAt()
                ))
                .toList();
        return new AuthSessionListResponse(items);
    }

    private String resolveSessionStatus(AuthSession session, Instant now) {
        if (session.status() == AuthSessionStatus.REVOKED) {
            return AuthSessionStatus.REVOKED.name();
        }
        if (!session.expiresAt().isAfter(now)) {
            return "EXPIRED";
        }
        return AuthSessionStatus.ACTIVE.name();
    }
}
