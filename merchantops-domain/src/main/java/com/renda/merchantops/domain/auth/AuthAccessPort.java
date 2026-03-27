package com.renda.merchantops.domain.auth;

import java.util.List;
import java.util.Optional;

public interface AuthAccessPort {

    Optional<TenantAccount> findTenantByCode(String tenantCode);

    Optional<TenantAccount> findTenantById(Long tenantId);

    Optional<AccessUserAccount> findUserByTenantIdAndUsername(Long tenantId, String username);

    Optional<AccessUserAccount> findUserByIdAndTenantId(Long userId, Long tenantId);

    List<String> findRoleCodes(Long userId, Long tenantId);

    List<String> findPermissionCodes(Long userId, Long tenantId);
}
