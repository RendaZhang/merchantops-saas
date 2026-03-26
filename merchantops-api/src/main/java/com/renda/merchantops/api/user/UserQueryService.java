package com.renda.merchantops.api.user;

import com.renda.merchantops.api.dto.user.query.UserDetailResponse;
import com.renda.merchantops.api.dto.user.query.UserListItemResponse;
import com.renda.merchantops.api.dto.user.query.UserPageQuery;
import com.renda.merchantops.api.dto.user.query.UserPageResponse;
import com.renda.merchantops.domain.user.UserDetail;
import com.renda.merchantops.domain.user.UserListItem;
import com.renda.merchantops.domain.user.UserPageCriteria;
import com.renda.merchantops.domain.user.UserQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserQueryService {

    private final UserQueryUseCase userQueryUseCase;

    public UserDetailResponse getUserDetail(Long tenantId, Long userId) {
        return toDetailResponse(userQueryUseCase.getUserDetail(tenantId, userId));
    }

    public List<UserListItemResponse> listUsers(Long tenantId) {
        return userQueryUseCase.listUsers(tenantId).stream()
                .map(this::toListItemResponse)
                .toList();
    }

    public List<UserListItemResponse> listUsersByStatus(Long tenantId, String status) {
        return userQueryUseCase.listUsersByStatus(tenantId, status).stream()
                .map(this::toListItemResponse)
                .toList();
    }

    public UserPageResponse pageUsers(Long tenantId, UserPageQuery query) {
        var result = userQueryUseCase.pageUsers(tenantId, toCriteria(query));
        return new UserPageResponse(
                result.items().stream().map(this::toListItemResponse).toList(),
                result.page(),
                result.size(),
                result.total(),
                result.totalPages()
        );
    }

    public boolean usernameExists(Long tenantId, String username) {
        return userQueryUseCase.usernameExists(tenantId, username);
    }

    public boolean usernameExists(Long tenantId, String username, Long excludeUserId) {
        return userQueryUseCase.usernameExists(tenantId, username, excludeUserId);
    }

    private UserPageCriteria toCriteria(UserPageQuery query) {
        if (query == null) {
            return null;
        }
        return new UserPageCriteria(
                query.getPage() == null ? -1 : query.getPage(),
                query.getSize() == null ? 0 : query.getSize(),
                query.getUsername(),
                query.getStatus(),
                query.getRoleCode()
        );
    }

    private UserListItemResponse toListItemResponse(UserListItem user) {
        return new UserListItemResponse(
                user.id(),
                user.username(),
                user.displayName(),
                user.email(),
                user.status()
        );
    }

    private UserDetailResponse toDetailResponse(UserDetail user) {
        return new UserDetailResponse(
                user.id(),
                user.tenantId(),
                user.username(),
                user.displayName(),
                user.email(),
                user.status(),
                user.roleCodes(),
                user.createdAt(),
                user.updatedAt()
        );
    }
}
