package com.renda.merchantops.domain.user;

import java.util.List;

public interface UserQueryUseCase {

    UserDetail getUserDetail(Long tenantId, Long userId);

    List<UserListItem> listUsers(Long tenantId);

    List<UserListItem> listUsersByStatus(Long tenantId, String status);

    UserPageResult pageUsers(Long tenantId, UserPageCriteria criteria);

    boolean usernameExists(Long tenantId, String username);

    boolean usernameExists(Long tenantId, String username, Long excludeUserId);
}
