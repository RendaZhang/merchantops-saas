package com.renda.merchantops.api.auth;

import com.renda.merchantops.api.config.AuthSessionCleanupProperties;
import com.renda.merchantops.domain.auth.AuthSessionUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthSessionCleanupSchedulerTest {

    @Mock
    private AuthSessionUseCase authSessionUseCase;

    @Test
    void cleanupOnceShouldDelegateUsingConfiguredRetentionAndBatchSize() {
        AuthSessionCleanupProperties properties = new AuthSessionCleanupProperties();
        properties.setEnabled(true);
        properties.setRetentionSeconds(604800);
        properties.setBatchSize(25);
        AuthSessionCleanupScheduler scheduler = new AuthSessionCleanupScheduler(authSessionUseCase, properties);
        LocalDateTime now = LocalDateTime.of(2026, 4, 22, 14, 30);
        when(authSessionUseCase.cleanupExpiredOrRevokedSessions(now, 604800, 25)).thenReturn(3);

        int deletedCount = scheduler.cleanupOnce(now);

        assertThat(deletedCount).isEqualTo(3);
        verify(authSessionUseCase).cleanupExpiredOrRevokedSessions(now, 604800, 25);
    }

    @Test
    void cleanupOnceShouldReturnZeroWithoutCallingUseCaseWhenDisabled() {
        AuthSessionCleanupProperties properties = new AuthSessionCleanupProperties();
        properties.setEnabled(false);
        AuthSessionCleanupScheduler scheduler = new AuthSessionCleanupScheduler(authSessionUseCase, properties);

        int deletedCount = scheduler.cleanupOnce(LocalDateTime.of(2026, 4, 22, 14, 30));

        assertThat(deletedCount).isZero();
        verify(authSessionUseCase, never()).cleanupExpiredOrRevokedSessions(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyInt());
    }
}
