package com.renda.merchantops.api.service;

import com.renda.merchantops.api.dto.user.command.UserCreateCommand;
import com.renda.merchantops.api.dto.user.command.UserCreateResponse;
import com.renda.merchantops.api.dto.user.command.UserPasswordUpdateCommand;
import com.renda.merchantops.api.dto.user.command.UserStatusUpdateCommand;
import com.renda.merchantops.api.dto.user.command.UserUpdateCommand;
import com.renda.merchantops.api.dto.user.command.UserWriteResponse;
import com.renda.merchantops.api.validation.PasswordRules;
import com.renda.merchantops.common.exception.BizException;
import com.renda.merchantops.common.exception.ErrorCode;
import com.renda.merchantops.infra.persistence.entity.RoleEntity;
import com.renda.merchantops.infra.persistence.entity.UserEntity;
import com.renda.merchantops.infra.persistence.entity.UserRoleEntity;
import com.renda.merchantops.infra.repository.RoleRepository;
import com.renda.merchantops.infra.repository.UserRepository;
import com.renda.merchantops.infra.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Write-side user service.
 * Immutable after creation: id, tenantId, username, createdAt.
 * Updatable by profile command: displayName, email.
 * Status changes are handled through a dedicated command.
 * Password changes are handled through a dedicated command.
 * System-managed only: updatedAt.
 */
@Service
@RequiredArgsConstructor
public class UserCommandService {

    private static final Set<String> ALLOWED_STATUSES = Set.of("ACTIVE", "DISABLED");

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    public void validateUsernameUnique(Long tenantId, String username) {
        if (userRepository.existsByTenantIdAndUsername(tenantId, username)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "username already exists in tenant");
        }
    }

    public void validateUsernameUnique(Long tenantId, String username, Long excludeUserId) {
        if (excludeUserId == null) {
            validateUsernameUnique(tenantId, username);
            return;
        }
        if (userRepository.existsByTenantIdAndUsernameAndIdNot(tenantId, username, excludeUserId)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "username already exists in tenant");
        }
    }

    @Transactional
    public UserCreateResponse createUser(Long tenantId, UserCreateCommand command) {
        String username = normalizeRequiredText(command == null ? null : command.getUsername(), "username");
        String displayName = normalizeRequiredText(command == null ? null : command.getDisplayName(), "displayName");
        String password = requireNonBlankPreserveValue(command == null ? null : command.getPassword(), "password");
        PasswordRules.requireNoBoundaryWhitespace(password);
        String email = normalizeOptionalText(command == null ? null : command.getEmail());
        List<String> roleCodes = normalizeRoleCodes(command == null ? null : command.getRoleCodes());

        validateUsernameUnique(tenantId, username);

        List<RoleEntity> roles = roleRepository.findAllByTenantIdAndRoleCodeInOrderByIdAsc(tenantId, roleCodes);
        if (roles.size() != roleCodes.size()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "roleCodes must exist in current tenant");
        }

        LocalDateTime now = LocalDateTime.now();
        UserEntity user = new UserEntity();
        user.setTenantId(tenantId);
        user.setUsername(username);
        user.setDisplayName(displayName);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setStatus("ACTIVE");
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        UserEntity savedUser;
        try {
            savedUser = userRepository.save(user);
        } catch (DataIntegrityViolationException ex) {
            throw new BizException(ErrorCode.BAD_REQUEST, "username already exists in tenant");
        }

        List<UserRoleEntity> userRoles = roles.stream()
                .map(role -> {
                    UserRoleEntity userRole = new UserRoleEntity();
                    userRole.setUserId(savedUser.getId());
                    userRole.setRoleId(role.getId());
                    return userRole;
                })
                .toList();
        userRoleRepository.saveAll(userRoles);

        return new UserCreateResponse(
                savedUser.getId(),
                savedUser.getTenantId(),
                savedUser.getUsername(),
                savedUser.getDisplayName(),
                savedUser.getEmail(),
                savedUser.getStatus(),
                roleCodes,
                savedUser.getCreatedAt(),
                savedUser.getUpdatedAt()
        );
    }

    @Transactional
    public UserWriteResponse updateUser(Long tenantId, Long userId, UserUpdateCommand command) {
        UserEntity user = requireTenantUser(tenantId, userId);
        user.setDisplayName(normalizeRequiredText(command == null ? null : command.getDisplayName(), "displayName"));
        user.setEmail(normalizeOptionalText(command == null ? null : command.getEmail()));
        user.setUpdatedAt(LocalDateTime.now());
        return toUserWriteResponse(userRepository.save(user));
    }

    @Transactional
    public UserWriteResponse updateStatus(Long tenantId, Long userId, UserStatusUpdateCommand command) {
        UserEntity user = requireTenantUser(tenantId, userId);
        user.setStatus(normalizeAllowedStatus(command == null ? null : command.getStatus()));
        user.setUpdatedAt(LocalDateTime.now());
        return toUserWriteResponse(userRepository.save(user));
    }

    public void updatePassword(Long tenantId, Long userId, UserPasswordUpdateCommand command) {
        requireTenantUser(tenantId, userId);
        throwFeatureNotReady("update password flow");
    }

    private UserEntity requireTenantUser(Long tenantId, Long userId) {
        return userRepository.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "user not found"));
    }

    private String normalizeRequiredText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new BizException(ErrorCode.BAD_REQUEST, fieldName + " must not be blank");
        }
        return value.trim();
    }

    private String normalizeOptionalText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String requireNonBlankPreserveValue(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new BizException(ErrorCode.BAD_REQUEST, fieldName + " must not be blank");
        }
        return value;
    }

    private String normalizeAllowedStatus(String value) {
        String status = normalizeRequiredText(value, "status");
        if (!ALLOWED_STATUSES.contains(status)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "status must be one of ACTIVE, DISABLED");
        }
        return status;
    }

    private List<String> normalizeRoleCodes(List<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "roleCodes must not be empty");
        }

        Set<String> normalized = new LinkedHashSet<>();
        for (String roleCode : roleCodes) {
            if (!StringUtils.hasText(roleCode)) {
                throw new BizException(ErrorCode.BAD_REQUEST, "roleCodes must not contain blank values");
            }
            normalized.add(roleCode.trim());
        }
        return List.copyOf(normalized);
    }

    private UserWriteResponse toUserWriteResponse(UserEntity user) {
        return new UserWriteResponse(
                user.getId(),
                user.getTenantId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getEmail(),
                user.getStatus(),
                user.getUpdatedAt()
        );
    }

    private void throwFeatureNotReady(String action) {
        throw new BizException(ErrorCode.BIZ_ERROR, action + " is not implemented yet");
    }
}
