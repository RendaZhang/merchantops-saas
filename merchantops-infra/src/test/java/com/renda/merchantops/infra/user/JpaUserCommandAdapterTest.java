package com.renda.merchantops.infra.user;

import com.renda.merchantops.domain.shared.error.BizException;
import com.renda.merchantops.domain.shared.error.ErrorCode;
import com.renda.merchantops.domain.user.NewUserDraft;
import com.renda.merchantops.infra.persistence.entity.UserEntity;
import com.renda.merchantops.infra.repository.UserRepository;
import com.renda.merchantops.infra.repository.UserRoleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaUserCommandAdapterTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Test
    void createUserShouldTranslateDuplicateUsernameConstraint() {
        JpaUserCommandAdapter adapter = new JpaUserCommandAdapter(userRepository, userRoleRepository);
        when(userRepository.save(org.mockito.ArgumentMatchers.any(UserEntity.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> adapter.createUser(new NewUserDraft(
                1L,
                "admin",
                "hash",
                "Admin",
                "admin@demo.local",
                "ACTIVE",
                LocalDateTime.now(),
                LocalDateTime.now(),
                101L,
                101L
        )))
                .isInstanceOf(BizException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BAD_REQUEST);
    }

    @Test
    void replaceUserRolesShouldDeleteOldRowsAndInsertRequestedBindings() {
        JpaUserCommandAdapter adapter = new JpaUserCommandAdapter(userRepository, userRoleRepository);

        adapter.replaceUserRoles(205L, java.util.List.of(12L, 13L));

        verify(userRoleRepository).deleteByUserId(205L);
        ArgumentCaptor<Iterable> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(userRoleRepository).saveAll(captor.capture());
        assertThat(StreamSupport.stream(captor.getValue().spliterator(), false).toList())
                .extracting("userId", "roleId")
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(205L, 12L),
                        org.assertj.core.groups.Tuple.tuple(205L, 13L)
                );
    }
}
