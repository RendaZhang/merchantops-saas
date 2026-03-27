package com.renda.merchantops.infra.auth;

import com.renda.merchantops.domain.auth.AccessUserAccount;
import com.renda.merchantops.domain.auth.AuthAccessPort;
import com.renda.merchantops.domain.auth.TenantAccount;
import com.renda.merchantops.infra.repository.PermissionRepository;
import com.renda.merchantops.infra.repository.RoleRepository;
import com.renda.merchantops.infra.repository.TenantRepository;
import com.renda.merchantops.infra.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class JpaAuthAccessAdapter implements AuthAccessPort {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public JpaAuthAccessAdapter(TenantRepository tenantRepository,
                                UserRepository userRepository,
                                RoleRepository roleRepository,
                                PermissionRepository permissionRepository) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    @Override
    public Optional<TenantAccount> findTenantByCode(String tenantCode) {
        return tenantRepository.findByTenantCode(tenantCode)
                .map(tenant -> new TenantAccount(
                        tenant.getId(),
                        tenant.getTenantCode(),
                        tenant.getStatus()
                ));
    }

    @Override
    public Optional<TenantAccount> findTenantById(Long tenantId) {
        return tenantRepository.findById(tenantId)
                .map(tenant -> new TenantAccount(
                        tenant.getId(),
                        tenant.getTenantCode(),
                        tenant.getStatus()
                ));
    }

    @Override
    public Optional<AccessUserAccount> findUserByTenantIdAndUsername(Long tenantId, String username) {
        return userRepository.findByTenantIdAndUsername(tenantId, username)
                .map(user -> new AccessUserAccount(
                        user.getId(),
                        user.getTenantId(),
                        user.getUsername(),
                        user.getPasswordHash(),
                        user.getStatus()
                ));
    }

    @Override
    public Optional<AccessUserAccount> findUserByIdAndTenantId(Long userId, Long tenantId) {
        return userRepository.findByIdAndTenantId(userId, tenantId)
                .map(user -> new AccessUserAccount(
                        user.getId(),
                        user.getTenantId(),
                        user.getUsername(),
                        user.getPasswordHash(),
                        user.getStatus()
                ));
    }

    @Override
    public List<String> findRoleCodes(Long userId, Long tenantId) {
        return roleRepository.findRolesByUserIdAndTenantId(userId, tenantId).stream()
                .map(role -> role.getRoleCode())
                .toList();
    }

    @Override
    public List<String> findPermissionCodes(Long userId, Long tenantId) {
        return permissionRepository.findPermissionsByUserIdAndTenantId(userId, tenantId).stream()
                .map(permission -> permission.getPermissionCode())
                .toList();
    }
}
