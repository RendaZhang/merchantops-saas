package com.renda.merchantops.infra.user;

import com.renda.merchantops.domain.shared.error.BizException;
import com.renda.merchantops.domain.shared.error.ErrorCode;
import com.renda.merchantops.domain.user.ManagedUser;
import com.renda.merchantops.domain.user.NewUserDraft;
import com.renda.merchantops.domain.user.UserCommandPort;
import com.renda.merchantops.infra.persistence.entity.UserEntity;
import com.renda.merchantops.infra.persistence.entity.UserRoleEntity;
import com.renda.merchantops.infra.repository.UserRepository;
import com.renda.merchantops.infra.repository.UserRoleRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class JpaUserCommandAdapter implements UserCommandPort {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    public JpaUserCommandAdapter(UserRepository userRepository, UserRoleRepository userRoleRepository) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
    }

    @Override
    public Optional<ManagedUser> findManagedUser(Long tenantId, Long userId) {
        return userRepository.findByIdAndTenantId(userId, tenantId).map(this::toManagedUser);
    }

    @Override
    public ManagedUser createUser(NewUserDraft draft) {
        try {
            return toManagedUser(userRepository.save(toNewEntity(draft)));
        } catch (DataIntegrityViolationException ex) {
            throw new BizException(ErrorCode.BAD_REQUEST, "username already exists in tenant");
        }
    }

    @Override
    public ManagedUser saveManagedUser(ManagedUser user) {
        return toManagedUser(userRepository.save(toExistingEntity(user)));
    }

    @Override
    public void replaceUserRoles(Long tenantId, Long userId, List<Long> roleIds) {
        userRoleRepository.deleteByTenantIdAndUserId(tenantId, userId);
        userRoleRepository.saveAll(roleIds.stream()
                .map(roleId -> {
                    UserRoleEntity userRole = new UserRoleEntity();
                    userRole.setUserId(userId);
                    userRole.setTenantId(tenantId);
                    userRole.setRoleId(roleId);
                    return userRole;
                })
                .toList());
    }

    private UserEntity toNewEntity(NewUserDraft draft) {
        UserEntity user = new UserEntity();
        user.setTenantId(draft.tenantId());
        user.setUsername(draft.username());
        user.setPasswordHash(draft.passwordHash());
        user.setDisplayName(draft.displayName());
        user.setEmail(draft.email());
        user.setStatus(draft.status());
        user.setCreatedAt(draft.createdAt());
        user.setUpdatedAt(draft.updatedAt());
        user.setCreatedBy(draft.createdBy());
        user.setUpdatedBy(draft.updatedBy());
        return user;
    }

    private UserEntity toExistingEntity(ManagedUser user) {
        UserEntity entity = new UserEntity();
        entity.setId(user.id());
        entity.setTenantId(user.tenantId());
        entity.setUsername(user.username());
        entity.setPasswordHash(user.passwordHash());
        entity.setDisplayName(user.displayName());
        entity.setEmail(user.email());
        entity.setStatus(user.status());
        entity.setCreatedAt(user.createdAt());
        entity.setUpdatedAt(user.updatedAt());
        entity.setCreatedBy(user.createdBy());
        entity.setUpdatedBy(user.updatedBy());
        return entity;
    }

    private ManagedUser toManagedUser(UserEntity user) {
        return new ManagedUser(
                user.getId(),
                user.getTenantId(),
                user.getUsername(),
                user.getPasswordHash(),
                user.getDisplayName(),
                user.getEmail(),
                user.getStatus(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getCreatedBy(),
                user.getUpdatedBy()
        );
    }
}
