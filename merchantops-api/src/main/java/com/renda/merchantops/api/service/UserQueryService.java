package com.renda.merchantops.api.service;

import com.renda.merchantops.api.dto.user.query.UserDetailResponse;
import com.renda.merchantops.api.dto.user.query.UserListItemResponse;
import com.renda.merchantops.api.dto.user.query.UserPageQuery;
import com.renda.merchantops.api.dto.user.query.UserPageResponse;
import com.renda.merchantops.common.exception.BizException;
import com.renda.merchantops.common.exception.ErrorCode;
import com.renda.merchantops.infra.persistence.entity.UserEntity;
import com.renda.merchantops.infra.repository.RoleRepository;
import com.renda.merchantops.infra.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserQueryService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 100;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public UserDetailResponse getUserDetail(Long tenantId, Long userId) {
        return userRepository.findByIdAndTenantId(userId, tenantId)
                .map(user -> toDetailResponse(user, tenantId))
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "user not found"));
    }

    public List<UserListItemResponse> listUsers(Long tenantId) {
        return userRepository.findAllByTenantIdOrderByIdAsc(tenantId)
                .stream()
                .map(this::toListItemResponse)
                .toList();
    }

    public List<UserListItemResponse> listUsersByStatus(Long tenantId, String status) {
        return userRepository.findAllByTenantIdAndStatusOrderByIdAsc(tenantId, status)
                .stream()
                .map(this::toListItemResponse)
                .toList();
    }

    public UserPageResponse pageUsers(Long tenantId, UserPageQuery query) {
        UserPageQuery normalizedQuery = normalizeQuery(query);
        PageRequest pageable = PageRequest.of(normalizedQuery.getPage(), normalizedQuery.getSize());

        Page<UserEntity> resultPage = userRepository.searchPageByTenantId(
                tenantId,
                normalizedQuery.getUsername(),
                normalizedQuery.getStatus(),
                normalizedQuery.getRoleCode(),
                pageable
        );

        return new UserPageResponse(
                resultPage.getContent().stream().map(this::toListItemResponse).toList(),
                resultPage.getNumber(),
                resultPage.getSize(),
                resultPage.getTotalElements(),
                resultPage.getTotalPages()
        );
    }

    public boolean usernameExists(Long tenantId, String username) {
        return userRepository.existsByTenantIdAndUsername(tenantId, username);
    }

    public boolean usernameExists(Long tenantId, String username, Long excludeUserId) {
        if (excludeUserId == null) {
            return usernameExists(tenantId, username);
        }
        return userRepository.existsByTenantIdAndUsernameAndIdNot(tenantId, username, excludeUserId);
    }

    private int normalizePage(UserPageQuery query) {
        if (query == null || query.getPage() == null || query.getPage() < 0) {
            return DEFAULT_PAGE;
        }
        return query.getPage();
    }

    private int normalizeSize(UserPageQuery query) {
        if (query == null || query.getSize() == null || query.getSize() <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(query.getSize(), MAX_SIZE);
    }

    private UserPageQuery normalizeQuery(UserPageQuery query) {
        UserPageQuery normalized = query == null ? new UserPageQuery() : query;
        normalized.setPage(normalizePage(query));
        normalized.setSize(normalizeSize(query));
        normalized.setUsername(normalizeFilter(normalized.getUsername()));
        normalized.setStatus(normalizeFilter(normalized.getStatus()));
        normalized.setRoleCode(normalizeFilter(normalized.getRoleCode()));
        return normalized;
    }

    private String normalizeFilter(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private UserListItemResponse toListItemResponse(UserEntity user) {
        return new UserListItemResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getEmail(),
                user.getStatus()
        );
    }

    private UserDetailResponse toDetailResponse(UserEntity user, Long tenantId) {
        return new UserDetailResponse(
                user.getId(),
                user.getTenantId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getEmail(),
                user.getStatus(),
                roleRepository.findRolesByUserIdAndTenantId(user.getId(), tenantId)
                        .stream()
                        .map(role -> role.getRoleCode())
                        .toList(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
