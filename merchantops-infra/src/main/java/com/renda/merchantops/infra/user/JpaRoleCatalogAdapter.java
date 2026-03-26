package com.renda.merchantops.infra.user;

import com.renda.merchantops.domain.user.RoleCatalogPort;
import com.renda.merchantops.domain.user.RoleItem;
import com.renda.merchantops.infra.repository.PermissionRepository;
import com.renda.merchantops.infra.repository.RoleRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JpaRoleCatalogAdapter implements RoleCatalogPort {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public JpaRoleCatalogAdapter(RoleRepository roleRepository, PermissionRepository permissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    @Override
    public List<RoleItem> listRoles(Long tenantId) {
        return roleRepository.findAllByTenantIdOrderByIdAsc(tenantId).stream()
                .map(role -> new RoleItem(role.getId(), role.getRoleCode(), role.getRoleName()))
                .toList();
    }

    @Override
    public List<RoleItem> findRolesByCodes(Long tenantId, List<String> roleCodes) {
        return roleRepository.findAllByTenantIdAndRoleCodeInOrderByIdAsc(tenantId, roleCodes).stream()
                .map(role -> new RoleItem(role.getId(), role.getRoleCode(), role.getRoleName()))
                .toList();
    }

    @Override
    public List<String> findRoleCodesByUserId(Long tenantId, Long userId) {
        return roleRepository.findRolesByUserIdAndTenantId(userId, tenantId).stream()
                .map(role -> role.getRoleCode())
                .toList();
    }

    @Override
    public List<String> findPermissionCodesByUserId(Long tenantId, Long userId) {
        return permissionRepository.findPermissionsByUserIdAndTenantId(userId, tenantId).stream()
                .map(permission -> permission.getPermissionCode())
                .toList();
    }
}
