package com.renda.merchantops.api.user;

import com.renda.merchantops.api.dto.user.query.UserDetailResponse;
import com.renda.merchantops.api.dto.user.query.UserListItemResponse;
import com.renda.merchantops.api.dto.user.query.UserPageQuery;
import com.renda.merchantops.api.dto.user.query.UserPageResponse;
import com.renda.merchantops.domain.user.UserDetail;
import com.renda.merchantops.domain.user.UserListItem;
import com.renda.merchantops.domain.user.UserPageCriteria;
import com.renda.merchantops.domain.user.UserPageResult;
import com.renda.merchantops.domain.user.UserQueryUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserQueryServiceTest {

    @Mock
    private UserQueryUseCase userQueryUseCase;

    @InjectMocks
    private UserQueryService userQueryService;

    @Test
    void getUserDetailShouldMapDomainResult() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 3, 10, 10, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 3, 10, 10, 30);
        when(userQueryUseCase.getUserDetail(2L, 3L)).thenReturn(new UserDetail(
                3L,
                2L,
                "viewer",
                "Viewer User",
                "viewer@demo-shop.local",
                "ACTIVE",
                List.of("READ_ONLY", "OPS_USER"),
                createdAt,
                updatedAt
        ));

        UserDetailResponse response = userQueryService.getUserDetail(2L, 3L);

        assertThat(response.getId()).isEqualTo(3L);
        assertThat(response.getTenantId()).isEqualTo(2L);
        assertThat(response.getUsername()).isEqualTo("viewer");
        assertThat(response.getDisplayName()).isEqualTo("Viewer User");
        assertThat(response.getEmail()).isEqualTo("viewer@demo-shop.local");
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
        assertThat(response.getRoleCodes()).containsExactly("READ_ONLY", "OPS_USER");
        assertThat(response.getCreatedAt()).isEqualTo(createdAt);
        assertThat(response.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void listUsersShouldMapDomainItems() {
        when(userQueryUseCase.listUsers(1L)).thenReturn(List.of(
                new UserListItem(1L, "admin", "Demo Admin", "admin@demo-shop.local", "ACTIVE"),
                new UserListItem(2L, "ops", "Ops User", "ops@demo-shop.local", "DISABLED")
        ));

        List<UserListItemResponse> response = userQueryService.listUsers(1L);

        assertThat(response)
                .extracting(UserListItemResponse::getUsername, UserListItemResponse::getStatus)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("admin", "ACTIVE"),
                        org.assertj.core.groups.Tuple.tuple("ops", "DISABLED")
                );
    }

    @Test
    void pageUsersShouldForwardCriteriaAndMapDomainPage() {
        UserPageQuery query = new UserPageQuery(-1, 999, "  adm  ", " ACTIVE ", " TENANT_ADMIN ");
        when(userQueryUseCase.pageUsers(eq(9L), any(UserPageCriteria.class))).thenReturn(new UserPageResult(
                List.of(new UserListItem(1L, "admin", "Demo Admin", "admin@demo-shop.local", "ACTIVE")),
                0,
                100,
                1,
                1
        ));

        UserPageResponse response = userQueryService.pageUsers(9L, query);

        ArgumentCaptor<UserPageCriteria> criteriaCaptor = ArgumentCaptor.forClass(UserPageCriteria.class);
        verify(userQueryUseCase).pageUsers(eq(9L), criteriaCaptor.capture());
        assertThat(criteriaCaptor.getValue().page()).isEqualTo(-1);
        assertThat(criteriaCaptor.getValue().size()).isEqualTo(999);
        assertThat(criteriaCaptor.getValue().username()).isEqualTo("  adm  ");
        assertThat(criteriaCaptor.getValue().status()).isEqualTo(" ACTIVE ");
        assertThat(criteriaCaptor.getValue().roleCode()).isEqualTo(" TENANT_ADMIN ");
        assertThat(response.getPage()).isEqualTo(0);
        assertThat(response.getSize()).isEqualTo(100);
        assertThat(response.getTotal()).isEqualTo(1);
        assertThat(response.getTotalPages()).isEqualTo(1);
        assertThat(response.getItems())
                .extracting(UserListItemResponse::getUsername)
                .containsExactly("admin");
    }

    @Test
    void pageUsersShouldPassNullCriteriaWhenQueryMissing() {
        when(userQueryUseCase.pageUsers(7L, null)).thenReturn(new UserPageResult(List.of(), 0, 10, 0, 0));

        UserPageResponse response = userQueryService.pageUsers(7L, null);

        verify(userQueryUseCase).pageUsers(7L, null);
        assertThat(response.getItems()).isEmpty();
        assertThat(response.getPage()).isEqualTo(0);
        assertThat(response.getSize()).isEqualTo(10);
        assertThat(response.getTotal()).isZero();
        assertThat(response.getTotalPages()).isZero();
    }

    @Test
    void usernameExistsShouldDelegateToUseCaseIncludingExcludeId() {
        when(userQueryUseCase.usernameExists(1L, "admin")).thenReturn(true);
        when(userQueryUseCase.usernameExists(1L, "admin", 9L)).thenReturn(false);

        assertThat(userQueryService.usernameExists(1L, "admin")).isTrue();
        assertThat(userQueryService.usernameExists(1L, "admin", 9L)).isFalse();
    }
}
