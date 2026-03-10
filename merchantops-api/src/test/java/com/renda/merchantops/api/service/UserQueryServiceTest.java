package com.renda.merchantops.api.service;

import com.renda.merchantops.api.dto.user.query.UserDetailResponse;
import com.renda.merchantops.api.dto.user.query.UserListItemResponse;
import com.renda.merchantops.api.dto.user.query.UserPageQuery;
import com.renda.merchantops.api.dto.user.query.UserPageResponse;
import com.renda.merchantops.common.exception.BizException;
import com.renda.merchantops.common.exception.ErrorCode;
import com.renda.merchantops.infra.persistence.entity.UserEntity;
import com.renda.merchantops.infra.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserQueryServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserQueryService userQueryService;

    @Test
    void pageUsersShouldNormalizeFiltersAndPageBounds() {
        UserPageQuery query = new UserPageQuery(-1, 999, "  adm  ", " ACTIVE ", " TENANT_ADMIN ");
        UserEntity user = user(1L, 9L, "admin", "Demo Admin", "admin@demo-shop.local", "ACTIVE");

        when(userRepository.searchPageByTenantId(eq(9L), eq("adm"), eq("ACTIVE"), eq("TENANT_ADMIN"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(user), PageRequest.of(0, 100), 1));

        UserPageResponse response = userQueryService.pageUsers(9L, query);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(userRepository).searchPageByTenantId(eq(9L), eq("adm"), eq("ACTIVE"), eq("TENANT_ADMIN"), pageableCaptor.capture());

        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(100);
        assertThat(response.getPage()).isEqualTo(0);
        assertThat(response.getSize()).isEqualTo(100);
        assertThat(response.getTotal()).isEqualTo(1);
        assertThat(response.getTotalPages()).isEqualTo(1);
        assertThat(response.getItems())
                .extracting(UserListItemResponse::getUsername)
                .containsExactly("admin");
    }

    @Test
    void pageUsersShouldUseDefaultsWhenQueryIsNull() {
        when(userRepository.searchPageByTenantId(eq(7L), eq(null), eq(null), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        UserPageResponse response = userQueryService.pageUsers(7L, null);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(userRepository).searchPageByTenantId(eq(7L), eq(null), eq(null), eq(null), pageableCaptor.capture());

        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(10);
        assertThat(response.getItems()).isEmpty();
        assertThat(response.getPage()).isEqualTo(0);
        assertThat(response.getSize()).isEqualTo(10);
    }

    @Test
    void listUsersShouldMapRepositoryResults() {
        when(userRepository.findAllByTenantIdOrderByIdAsc(1L)).thenReturn(List.of(
                user(1L, 1L, "admin", "Demo Admin", "admin@demo-shop.local", "ACTIVE"),
                user(2L, 1L, "ops", "Ops User", "ops@demo-shop.local", "ACTIVE")
        ));

        List<UserListItemResponse> response = userQueryService.listUsers(1L);

        assertThat(response)
                .extracting(UserListItemResponse::getUsername)
                .containsExactly("admin", "ops");
    }

    @Test
    void listUsersByStatusShouldMapRepositoryResults() {
        when(userRepository.findAllByTenantIdAndStatusOrderByIdAsc(1L, "ACTIVE")).thenReturn(List.of(
                user(3L, 1L, "viewer", "Viewer User", "viewer@demo-shop.local", "ACTIVE")
        ));

        List<UserListItemResponse> response = userQueryService.listUsersByStatus(1L, "ACTIVE");

        assertThat(response)
                .extracting(UserListItemResponse::getUsername)
                .containsExactly("viewer");
    }

    @Test
    void getUserDetailShouldMapEntity() {
        UserEntity user = user(3L, 2L, "viewer", "Viewer User", "viewer@demo-shop.local", "ACTIVE");
        when(userRepository.findByIdAndTenantId(3L, 2L)).thenReturn(Optional.of(user));

        UserDetailResponse response = userQueryService.getUserDetail(2L, 3L);

        assertThat(response.getId()).isEqualTo(3L);
        assertThat(response.getTenantId()).isEqualTo(2L);
        assertThat(response.getUsername()).isEqualTo("viewer");
        assertThat(response.getDisplayName()).isEqualTo("Viewer User");
        assertThat(response.getEmail()).isEqualTo("viewer@demo-shop.local");
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
        assertThat(response.getCreatedAt()).isEqualTo(user.getCreatedAt());
        assertThat(response.getUpdatedAt()).isEqualTo(user.getUpdatedAt());
    }

    @Test
    void usernameExistsShouldDelegateToRepositoryIncludingExcludeId() {
        when(userRepository.existsByTenantIdAndUsername(1L, "admin")).thenReturn(true);
        when(userRepository.existsByTenantIdAndUsernameAndIdNot(1L, "admin", 9L)).thenReturn(false);

        assertThat(userQueryService.usernameExists(1L, "admin")).isTrue();
        assertThat(userQueryService.usernameExists(1L, "admin", 9L)).isFalse();
    }

    @Test
    void getUserDetailShouldThrowNotFoundWhenUserMissing() {
        when(userRepository.findByIdAndTenantId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userQueryService.getUserDetail(1L, 99L))
                .isInstanceOf(BizException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    private UserEntity user(Long id,
                            Long tenantId,
                            String username,
                            String displayName,
                            String email,
                            String status) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setTenantId(tenantId);
        user.setUsername(username);
        user.setDisplayName(displayName);
        user.setEmail(email);
        user.setStatus(status);
        user.setCreatedAt(LocalDateTime.of(2026, 3, 10, 10, 0));
        user.setUpdatedAt(LocalDateTime.of(2026, 3, 10, 10, 30));
        return user;
    }
}
