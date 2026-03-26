package com.renda.merchantops.infra.user;

import com.renda.merchantops.domain.user.UserDetail;
import com.renda.merchantops.domain.user.UserListItem;
import com.renda.merchantops.domain.user.UserPageCriteria;
import com.renda.merchantops.domain.user.UserPageResult;
import com.renda.merchantops.domain.user.UserQueryPort;
import com.renda.merchantops.infra.persistence.entity.UserEntity;
import com.renda.merchantops.infra.repository.RoleRepository;
import com.renda.merchantops.infra.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class JpaUserQueryAdapter implements UserQueryPort {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public JpaUserQueryAdapter(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    public Optional<UserDetail> findUserDetail(Long tenantId, Long userId) {
        return userRepository.findByIdAndTenantId(userId, tenantId)
                .map(user -> new UserDetail(
                        user.getId(),
                        user.getTenantId(),
                        user.getUsername(),
                        user.getDisplayName(),
                        user.getEmail(),
                        user.getStatus(),
                        roleRepository.findRolesByUserIdAndTenantId(user.getId(), tenantId).stream()
                                .map(role -> role.getRoleCode())
                                .toList(),
                        user.getCreatedAt(),
                        user.getUpdatedAt()
                ));
    }

    @Override
    public List<UserListItem> listUsers(Long tenantId) {
        return userRepository.findAllByTenantIdOrderByIdAsc(tenantId).stream()
                .map(user -> new UserListItem(
                        user.getId(),
                        user.getUsername(),
                        user.getDisplayName(),
                        user.getEmail(),
                        user.getStatus()
                ))
                .toList();
    }

    @Override
    public List<UserListItem> listUsersByStatus(Long tenantId, String status) {
        return userRepository.findAllByTenantIdAndStatusOrderByIdAsc(tenantId, status).stream()
                .map(user -> new UserListItem(
                        user.getId(),
                        user.getUsername(),
                        user.getDisplayName(),
                        user.getEmail(),
                        user.getStatus()
                ))
                .toList();
    }

    @Override
    public UserPageResult pageUsers(Long tenantId, UserPageCriteria criteria) {
        Page<UserEntity> resultPage = userRepository.searchPageByTenantId(
                tenantId,
                criteria.username(),
                criteria.status(),
                criteria.roleCode(),
                PageRequest.of(criteria.page(), criteria.size())
        );
        return new UserPageResult(
                resultPage.getContent().stream()
                        .map(user -> new UserListItem(
                                user.getId(),
                                user.getUsername(),
                                user.getDisplayName(),
                                user.getEmail(),
                                user.getStatus()
                        ))
                        .toList(),
                resultPage.getNumber(),
                resultPage.getSize(),
                resultPage.getTotalElements(),
                resultPage.getTotalPages()
        );
    }

    @Override
    public boolean usernameExists(Long tenantId, String username) {
        return userRepository.existsByTenantIdAndUsername(tenantId, username);
    }

    @Override
    public boolean usernameExists(Long tenantId, String username, Long excludeUserId) {
        return userRepository.existsByTenantIdAndUsernameAndIdNot(tenantId, username, excludeUserId);
    }
}
