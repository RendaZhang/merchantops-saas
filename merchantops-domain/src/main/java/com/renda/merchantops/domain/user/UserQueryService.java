package com.renda.merchantops.domain.user;

import com.renda.merchantops.domain.shared.error.BizException;
import com.renda.merchantops.domain.shared.error.ErrorCode;

import java.util.List;

public final class UserQueryService implements UserQueryUseCase {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 100;

    private final UserQueryPort userQueryPort;

    public UserQueryService(UserQueryPort userQueryPort) {
        this.userQueryPort = userQueryPort;
    }

    @Override
    public UserDetail getUserDetail(Long tenantId, Long userId) {
        return userQueryPort.findUserDetail(tenantId, userId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "user not found"));
    }

    @Override
    public List<UserListItem> listUsers(Long tenantId) {
        return userQueryPort.listUsers(tenantId);
    }

    @Override
    public List<UserListItem> listUsersByStatus(Long tenantId, String status) {
        return userQueryPort.listUsersByStatus(tenantId, status);
    }

    @Override
    public UserPageResult pageUsers(Long tenantId, UserPageCriteria criteria) {
        return userQueryPort.pageUsers(tenantId, normalizeCriteria(criteria));
    }

    @Override
    public boolean usernameExists(Long tenantId, String username) {
        return userQueryPort.usernameExists(tenantId, username);
    }

    @Override
    public boolean usernameExists(Long tenantId, String username, Long excludeUserId) {
        if (excludeUserId == null) {
            return usernameExists(tenantId, username);
        }
        return userQueryPort.usernameExists(tenantId, username, excludeUserId);
    }

    private UserPageCriteria normalizeCriteria(UserPageCriteria criteria) {
        if (criteria == null) {
            return new UserPageCriteria(DEFAULT_PAGE, DEFAULT_SIZE, null, null, null);
        }
        return new UserPageCriteria(
                normalizePage(criteria.page()),
                normalizeSize(criteria.size()),
                normalizeFilter(criteria.username()),
                normalizeFilter(criteria.status()),
                normalizeFilter(criteria.roleCode())
        );
    }

    private int normalizePage(int page) {
        return page < 0 ? DEFAULT_PAGE : page;
    }

    private int normalizeSize(int size) {
        if (size <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    private String normalizeFilter(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
