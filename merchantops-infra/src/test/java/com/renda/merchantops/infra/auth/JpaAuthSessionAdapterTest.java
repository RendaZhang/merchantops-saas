package com.renda.merchantops.infra.auth;

import com.renda.merchantops.infra.repository.AuthSessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaAuthSessionAdapterTest {

    @Mock
    private AuthSessionRepository authSessionRepository;

    @Test
    void cleanupExpiredOrRevokedSessionsShouldQueryOnePageAndDeleteCandidateIds() {
        JpaAuthSessionAdapter adapter = new JpaAuthSessionAdapter(authSessionRepository);
        LocalDateTime cutoff = LocalDateTime.of(2026, 4, 15, 10, 0);
        when(authSessionRepository.findCleanupCandidateIds(
                eq("ACTIVE"),
                eq("REVOKED"),
                eq(cutoff),
                any(Pageable.class)
        )).thenReturn(List.of(11L, 12L));
        when(authSessionRepository.deleteByIdIn(List.of(11L, 12L))).thenReturn(2);

        int deletedCount = adapter.cleanupExpiredOrRevokedSessions(cutoff, 25);

        assertThat(deletedCount).isEqualTo(2);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(authSessionRepository).findCleanupCandidateIds(
                eq("ACTIVE"),
                eq("REVOKED"),
                eq(cutoff),
                pageableCaptor.capture()
        );
        assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(25);
        verify(authSessionRepository).deleteByIdIn(List.of(11L, 12L));
    }

    @Test
    void cleanupExpiredOrRevokedSessionsShouldSkipDeleteWhenNoCandidatesExist() {
        JpaAuthSessionAdapter adapter = new JpaAuthSessionAdapter(authSessionRepository);
        LocalDateTime cutoff = LocalDateTime.of(2026, 4, 15, 10, 0);
        when(authSessionRepository.findCleanupCandidateIds(
                eq("ACTIVE"),
                eq("REVOKED"),
                eq(cutoff),
                any(Pageable.class)
        )).thenReturn(List.of());

        int deletedCount = adapter.cleanupExpiredOrRevokedSessions(cutoff, 25);

        assertThat(deletedCount).isZero();
        verify(authSessionRepository, never()).deleteByIdIn(any());
    }
}
