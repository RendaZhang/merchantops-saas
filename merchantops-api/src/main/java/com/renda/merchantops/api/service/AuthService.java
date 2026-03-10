package com.renda.merchantops.api.service;

import com.renda.merchantops.api.dto.auth.LoginRequest;
import com.renda.merchantops.api.dto.auth.LoginResponse;
import com.renda.merchantops.api.security.JwtTokenService;
import com.renda.merchantops.api.validation.PasswordRules;
import com.renda.merchantops.common.exception.BizException;
import com.renda.merchantops.common.exception.ErrorCode;
import com.renda.merchantops.infra.persistence.entity.PermissionEntity;
import com.renda.merchantops.infra.persistence.entity.RoleEntity;
import com.renda.merchantops.infra.persistence.entity.TenantEntity;
import com.renda.merchantops.infra.persistence.entity.UserEntity;
import com.renda.merchantops.infra.repository.PermissionRepository;
import com.renda.merchantops.infra.repository.RoleRepository;
import com.renda.merchantops.infra.repository.TenantRepository;
import com.renda.merchantops.infra.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public LoginResponse login(LoginRequest request) {
        PasswordRules.requireNoBoundaryWhitespace(request.getPassword());

        TenantEntity tenant = tenantRepository.findByTenantCode(request.getTenantCode())
                .orElseThrow(() -> new BizException(ErrorCode.BAD_REQUEST, "tenant not found"));

        if (!"ACTIVE".equalsIgnoreCase(tenant.getStatus())) {
            throw new BizException(ErrorCode.FORBIDDEN, "tenant is not active");
        }

        UserEntity user = userRepository.findByTenantIdAndUsername(tenant.getId(), request.getUsername())
                .orElseThrow(() -> new BizException(ErrorCode.BAD_REQUEST, "username or password is incorrect"));

        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new BizException(ErrorCode.FORBIDDEN, "user is not active");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "username or password is incorrect");
        }

        List<String> roles = roleRepository.findRolesByUserIdAndTenantId(user.getId(), tenant.getId())
                .stream()
                .map(RoleEntity::getRoleCode)
                .toList();

        List<String> permissions = permissionRepository.findPermissionsByUserIdAndTenantId(user.getId(), tenant.getId())
                .stream()
                .map(PermissionEntity::getPermissionCode)
                .toList();

        String token = jwtTokenService.generateToken(
                user.getId(),
                tenant.getId(),
                tenant.getTenantCode(),
                user.getUsername(),
                roles,
                permissions
        );

        return new LoginResponse(token, "Bearer", jwtTokenService.getExpireSeconds());
    }

}
