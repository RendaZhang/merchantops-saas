package com.renda.merchantops.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.renda.merchantops.api.context.CurrentUserContext;
import com.renda.merchantops.api.context.TenantContext;
import com.renda.merchantops.domain.shared.error.ErrorCode;
import com.renda.merchantops.api.platform.response.ApiResponse;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenService jwtTokenService;
    private final CurrentUserAccessValidator currentUserAccessValidator;
    private final ObjectMapper objectMapper;
    private final RequestMatcher publicEndpointMatcher = new OrRequestMatcher(
            new AntPathRequestMatcher("/health"),
            new AntPathRequestMatcher("/actuator/**"),
            new AntPathRequestMatcher("/swagger-ui/**"),
            new AntPathRequestMatcher("/swagger-ui.html"),
            new AntPathRequestMatcher("/v3/api-docs/**"),
            new AntPathRequestMatcher("/api/v1/dev/**"),
            new AntPathRequestMatcher("/api/v1/auth/login")
    );

    public JwtAuthenticationFilter(JwtTokenService jwtTokenService,
                                   CurrentUserAccessValidator currentUserAccessValidator,
                                   ObjectMapper objectMapper) {
        this.jwtTokenService = jwtTokenService;
        this.currentUserAccessValidator = currentUserAccessValidator;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return publicEndpointMatcher.matches(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        try {
            if (StringUtils.hasText(authHeader) && authHeader.startsWith(BEARER_PREFIX)) {
                String token = authHeader.substring(BEARER_PREFIX.length());

                try {
                    CurrentUser currentUser = jwtTokenService.parseCurrentUser(token);
                    CurrentUserAccessValidator.ValidationResult validationResult = currentUserAccessValidator.validate(currentUser);
                    if (validationResult.status() == CurrentUserAccessValidator.Status.TENANT_INACTIVE) {
                        writeForbiddenResponse(response, "tenant is not active");
                        return;
                    }
                    if (validationResult.status() == CurrentUserAccessValidator.Status.USER_INACTIVE) {
                        writeForbiddenResponse(response, "user is not active");
                        return;
                    }
                    if (validationResult.status() == CurrentUserAccessValidator.Status.CLAIMS_STALE) {
                        writeForbiddenResponse(response, "token claims are stale, please login again");
                        return;
                    }
                    if (validationResult.status() == CurrentUserAccessValidator.Status.USER_MISSING) {
                        SecurityContextHolder.clearContext();
                        filterChain.doFilter(request, response);
                        return;
                    }
                    CurrentUser validatedCurrentUser = validationResult.currentUser();

                    List<SimpleGrantedAuthority> authorities = Stream.concat(
                                    validatedCurrentUser.getRoles().stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role)),
                                    validatedCurrentUser.getPermissions().stream().map(SimpleGrantedAuthority::new)
                            )
                            .toList();

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(validatedCurrentUser, null, authorities);

                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    CurrentUserContext.set(validatedCurrentUser);
                    TenantContext.setTenant(validatedCurrentUser.getTenantId(), validatedCurrentUser.getTenantCode());
                } catch (JwtException | IllegalArgumentException ex) {
                    SecurityContextHolder.clearContext();
                }
            }

            filterChain.doFilter(request, response);
        } finally {
            CurrentUserContext.clear();
            TenantContext.clear();
        }
    }

    private void writeForbiddenResponse(HttpServletResponse response, String message) throws IOException {
        SecurityContextHolder.clearContext();
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiResponse<Void> body = ApiResponse.failure(ErrorCode.FORBIDDEN, message);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
