package com.renda.merchantops.domain.user;

import java.util.List;

public interface RoleCatalogPort {

    List<RoleItem> listRoles(Long tenantId);

    List<RoleItem> findRolesByCodes(Long tenantId, List<String> roleCodes);

    List<String> findRoleCodesByUserId(Long tenantId, Long userId);

    List<String> findPermissionCodesByUserId(Long tenantId, Long userId);
}
