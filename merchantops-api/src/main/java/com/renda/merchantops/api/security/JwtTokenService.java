package com.renda.merchantops.api.security;

import com.renda.merchantops.api.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Service
public class JwtTokenService {

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public JwtTokenService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Long userId,
                                Long tenantId,
                                String tenantCode,
                                String username,
                                List<String> roles,
                                List<String> permissions) {

        Instant now = Instant.now();
        Instant expireAt = now.plusSeconds(jwtProperties.getExpireSeconds());

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("tenantId", tenantId)
                .claim("tenantCode", tenantCode)
                .claim("username", username)
                .claim("roles", roles)
                .claim("permissions", permissions)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expireAt))
                .signWith(secretKey)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public CurrentUser parseCurrentUser(String token) {
        Claims claims = parseClaims(token);

        Long userId = parseRequiredLong(claims.getSubject(), "sub");
        Long tenantId = parseRequiredLong(claims.get("tenantId"), "tenantId");
        String tenantCode = claims.get("tenantCode", String.class);
        String username = claims.get("username", String.class);
        if (!StringUtils.hasText(tenantCode)) {
            throw new IllegalArgumentException("missing claim: tenantCode");
        }
        if (!StringUtils.hasText(username)) {
            throw new IllegalArgumentException("missing claim: username");
        }

        List<String> roles = toStringList(claims.get("roles"));
        List<String> permissions = toStringList(claims.get("permissions"));

        return new CurrentUser(
                userId,
                tenantId,
                tenantCode,
                username,
                roles,
                permissions
        );
    }

    public long getExpireSeconds() {
        return jwtProperties.getExpireSeconds();
    }

    private List<String> toStringList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return Collections.emptyList();
    }

    private Long parseRequiredLong(Object value, String claimName) {
        if (value == null) {
            throw new IllegalArgumentException("missing claim: " + claimName);
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("invalid claim: " + claimName, ex);
        }
    }

}
