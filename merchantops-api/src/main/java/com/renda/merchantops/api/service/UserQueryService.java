package com.renda.merchantops.api.service;

import com.renda.merchantops.api.dto.user.query.UserDetailResponse;
import com.renda.merchantops.api.dto.user.query.UserListItemResponse;
import com.renda.merchantops.api.dto.user.query.UserPageQuery;
import com.renda.merchantops.api.dto.user.query.UserPageResponse;
import com.renda.merchantops.common.exception.BizException;
import com.renda.merchantops.common.exception.ErrorCode;
import com.renda.merchantops.infra.persistence.entity.UserEntity;
import com.renda.merchantops.infra.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserQueryService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final UserRepository userRepository;

    public UserDetailResponse getUserDetail(Long tenantId, Long userId) {
        return userRepository.findByIdAndTenantId(userId, tenantId)
                .map(this::toDetailResponse)
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
        PageRequest pageable = PageRequest.of(
                normalizePage(query),
                normalizeSize(query),
                Sort.by(Sort.Direction.ASC, "id")
        );

        Page<UserEntity> resultPage = hasStatusFilter(query)
                ? userRepository.findAllByTenantIdAndStatus(tenantId, query.getStatus(), pageable)
                : userRepository.findAllByTenantId(tenantId, pageable);

        return new UserPageResponse(
                resultPage.getNumber(),
                resultPage.getSize(),
                resultPage.getTotalElements(),
                resultPage.getTotalPages(),
                resultPage.hasNext(),
                resultPage.getContent().stream().map(this::toListItemResponse).toList()
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

    private boolean hasStatusFilter(UserPageQuery query) {
        return query != null && StringUtils.hasText(query.getStatus());
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

    private UserListItemResponse toListItemResponse(UserEntity user) {
        return new UserListItemResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getEmail(),
                user.getStatus()
        );
    }

    private UserDetailResponse toDetailResponse(UserEntity user) {
        return new UserDetailResponse(
                user.getId(),
                user.getTenantId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getEmail(),
                user.getStatus(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
