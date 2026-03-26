package com.renda.merchantops.domain.user;

import com.renda.merchantops.domain.shared.error.BizException;
import com.renda.merchantops.domain.shared.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserQueryServiceTest {

    @Mock
    private UserQueryPort userQueryPort;

    @Test
    void pageUsersShouldNormalizeQueryBeforeDelegating() {
        UserQueryService service = new UserQueryService(userQueryPort);
        when(userQueryPort.pageUsers(9L, new UserPageCriteria(0, 100, "adm", "ACTIVE", "TENANT_ADMIN")))
                .thenReturn(new UserPageResult(List.of(), 0, 100, 0, 0));

        service.pageUsers(9L, new UserPageCriteria(-1, 999, "  adm  ", " ACTIVE ", " TENANT_ADMIN "));

        ArgumentCaptor<UserPageCriteria> criteriaCaptor = ArgumentCaptor.forClass(UserPageCriteria.class);
        verify(userQueryPort).pageUsers(org.mockito.ArgumentMatchers.eq(9L), criteriaCaptor.capture());
        assertThat(criteriaCaptor.getValue()).isEqualTo(new UserPageCriteria(0, 100, "adm", "ACTIVE", "TENANT_ADMIN"));
    }

    @Test
    void getUserDetailShouldThrowNotFoundWhenMissing() {
        UserQueryService service = new UserQueryService(userQueryPort);
        when(userQueryPort.findUserDetail(1L, 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getUserDetail(1L, 99L))
                .isInstanceOf(BizException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOT_FOUND);
    }
}
