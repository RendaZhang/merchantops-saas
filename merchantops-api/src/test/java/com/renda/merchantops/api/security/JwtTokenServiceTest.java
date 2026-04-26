package com.renda.merchantops.api.security;

import com.renda.merchantops.api.config.JwtProperties;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenServiceTest {

    @Test
    void generateTokenShouldPreserveProvidedInstantsRegardlessOfJvmTimezone() {
        TimeZone originalTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
        try {
            JwtProperties properties = new JwtProperties();
            properties.setSecret("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
            properties.setExpireSeconds(7200);
            JwtTokenService jwtTokenService = new JwtTokenService(properties);
            Instant issuedAt = Instant.now().minusSeconds(60).truncatedTo(ChronoUnit.SECONDS);
            Instant expiresAt = issuedAt.plusSeconds(7200);

            String token = jwtTokenService.generateToken(
                    101L,
                    1L,
                    "demo-shop",
                    "admin",
                    List.of("SUPER_ADMIN"),
                    List.of("USER_READ"),
                    "session-1",
                    issuedAt,
                    expiresAt
            );

            Claims claims = jwtTokenService.parseClaims(token);

            assertThat(claims.getIssuedAt().toInstant()).isEqualTo(issuedAt);
            assertThat(claims.getExpiration().toInstant()).isEqualTo(expiresAt);
        } finally {
            TimeZone.setDefault(originalTimeZone);
        }
    }
}
