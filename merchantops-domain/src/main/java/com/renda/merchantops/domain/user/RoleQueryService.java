package com.renda.merchantops.domain.user;

import java.util.List;

public final class RoleQueryService implements RoleQueryUseCase {

    private final RoleCatalogPort roleCatalogPort;

    public RoleQueryService(RoleCatalogPort roleCatalogPort) {
        this.roleCatalogPort = roleCatalogPort;
    }

    @Override
    public List<RoleItem> listRoles(Long tenantId) {
        return roleCatalogPort.listRoles(tenantId);
    }
}
