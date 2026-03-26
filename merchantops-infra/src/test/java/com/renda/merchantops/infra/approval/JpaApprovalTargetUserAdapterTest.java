package com.renda.merchantops.infra.approval;

import com.renda.merchantops.infra.persistence.entity.UserEntity;
import com.renda.merchantops.infra.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaApprovalTargetUserAdapterTest {

    @Mock
    private UserRepository userRepository;

    @Test
    void findForDisableShouldMapLockedUserIntoDomainTarget() {
        JpaApprovalTargetUserAdapter adapter = new JpaApprovalTargetUserAdapter(userRepository);
        UserEntity user = new UserEntity();
        user.setId(103L);
        user.setStatus("ACTIVE");
        when(userRepository.findByIdAndTenantIdForUpdate(103L, 1L)).thenReturn(Optional.of(user));

        var result = adapter.findForDisable(1L, 103L);

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().status()).isEqualTo("ACTIVE");
    }
}
