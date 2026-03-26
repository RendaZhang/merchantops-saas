package com.renda.merchantops.domain.user;

import java.util.List;

public interface RoleQueryUseCase {

    List<RoleItem> listRoles(Long tenantId);
}
