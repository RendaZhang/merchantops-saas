package com.renda.merchantops.domain.user;

import java.util.List;
import java.util.Optional;

public interface UserCommandPort {

    Optional<ManagedUser> findManagedUser(Long tenantId, Long userId);

    ManagedUser createUser(NewUserDraft draft);

    ManagedUser saveManagedUser(ManagedUser user);

    void replaceUserRoles(Long tenantId, Long userId, List<Long> roleIds);
}
