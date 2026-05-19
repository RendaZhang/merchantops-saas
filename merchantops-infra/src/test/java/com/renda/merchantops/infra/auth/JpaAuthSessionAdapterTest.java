package com.renda.merchantops.infra.auth;

import com.renda.merchantops.domain.auth.AuthSession;
import com.renda.merchantops.infra.persistence.entity.AuthSessionEntity;
import com.renda.merchantops.infra.repository.AuthSessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
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
    void createShouldPersistUtcInstantsAndMapThemBackToDomain() {
        JpaAuthSessionAdapter adapter = new JpaAuthSessionAdapter(authSessionRepository);
        Instant createdAt = Instant.parse("2026-04-23T10:30:15Z");
        Instant expiresAt = Instant.parse("2026-04-23T12:30:15Z");
        when(authSessionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AuthSession authSession = adapter.create("session-1", 1L, 101L, createdAt, expiresAt);

        ArgumentCaptor<com.renda.merchantops.infra.persistence.entity.AuthSessionEntity> entityCaptor =
                ArgumentCaptor.forClass(com.renda.merchantops.infra.persistence.entity.AuthSessionEntity.class);
        verify(authSessionRepository).save(entityCaptor.capture());
        assertThat(entityCaptor.getValue().getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 4, 23, 10, 30, 15));
        assertThat(entityCaptor.getValue().getExpiresAt()).isEqualTo(LocalDateTime.of(2026, 4, 23, 12, 30, 15));
        assertThat(authSession.createdAt()).isEqualTo(createdAt);
        assertThat(authSession.expiresAt()).isEqualTo(expiresAt);
    }

    @Test
    void revokeActiveSessionsForUserShouldDelegateTenantScopedBulkUpdate() {
        JpaAuthSessionAdapter adapter = new JpaAuthSessionAdapter(authSessionRepository);
        Instant revokedAt = Instant.parse("2026-04-23T10:30:00Z");
        LocalDateTime utcRevokedAt = LocalDateTime.of(2026, 4, 23, 10, 30);
        when(authSessionRepository.revokeActiveSessionsForUser(
                1L,
                101L,
                "ACTIVE",
                "REVOKED",
                utcRevokedAt
        )).thenReturn(2);

        int revokedCount = adapter.revokeActiveSessionsForUser(1L, 101L, revokedAt);

        assertThat(revokedCount).isEqualTo(2);
        verify(authSessionRepository).revokeActiveSessionsForUser(
                1L,
                101L,
                "ACTIVE",
                "REVOKED",
                utcRevokedAt
        );
    }

    @Test
    void findAllByTenantIdAndUserIdShouldMapRepositoryRowsInProvidedOrder() {
        JpaAuthSessionAdapter adapter = new JpaAuthSessionAdapter(authSessionRepository);
        AuthSessionEntity newer = authSessionEntity(
                12L,
                "newer-session",
                LocalDateTime.of(2026, 5, 19, 10, 30),
                LocalDateTime.of(2026, 5, 19, 12, 30),
                null
        );
        AuthSessionEntity olderRevoked = authSessionEntity(
                11L,
                "older-session",
                LocalDateTime.of(2026, 5, 18, 10, 30),
                LocalDateTime.of(2026, 5, 18, 12, 30),
                LocalDateTime.of(2026, 5, 18, 11, 0)
        );
        olderRevoked.setStatus("REVOKED");
        when(authSessionRepository.findAllByTenantIdAndUserIdOrderByCreatedAtDescIdDesc(1L, 101L))
                .thenReturn(List.of(newer, olderRevoked));

        List<AuthSession> sessions = adapter.findAllByTenantIdAndUserId(1L, 101L);

        assertThat(sessions).extracting(AuthSession::sessionId)
                .containsExactly("newer-session", "older-session");
        assertThat(sessions.get(0).createdAt()).isEqualTo(Instant.parse("2026-05-19T10:30:00Z"));
        assertThat(sessions.get(1).revokedAt()).isEqualTo(Instant.parse("2026-05-18T11:00:00Z"));
        verify(authSessionRepository).findAllByTenantIdAndUserIdOrderByCreatedAtDescIdDesc(1L, 101L);
    }

    @Test
    void cleanupExpiredOrRevokedSessionsShouldQueryOnePageAndDeleteCandidateIds() {
        JpaAuthSessionAdapter adapter = new JpaAuthSessionAdapter(authSessionRepository);
        Instant cutoff = Instant.parse("2026-04-15T10:00:00Z");
        LocalDateTime utcCutoff = LocalDateTime.of(2026, 4, 15, 10, 0);
        when(authSessionRepository.findCleanupCandidateIds(
                eq("ACTIVE"),
                eq("REVOKED"),
                eq(utcCutoff),
                any(Pageable.class)
        )).thenReturn(List.of(11L, 12L));
        when(authSessionRepository.deleteByIdIn(List.of(11L, 12L))).thenReturn(2);

        int deletedCount = adapter.cleanupExpiredOrRevokedSessions(cutoff, 25);

        assertThat(deletedCount).isEqualTo(2);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(authSessionRepository).findCleanupCandidateIds(
                eq("ACTIVE"),
                eq("REVOKED"),
                eq(utcCutoff),
                pageableCaptor.capture()
        );
        assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(25);
        verify(authSessionRepository).deleteByIdIn(List.of(11L, 12L));
    }

    @Test
    void cleanupExpiredOrRevokedSessionsShouldSkipDeleteWhenNoCandidatesExist() {
        JpaAuthSessionAdapter adapter = new JpaAuthSessionAdapter(authSessionRepository);
        Instant cutoff = Instant.parse("2026-04-15T10:00:00Z");
        LocalDateTime utcCutoff = LocalDateTime.of(2026, 4, 15, 10, 0);
        when(authSessionRepository.findCleanupCandidateIds(
                eq("ACTIVE"),
                eq("REVOKED"),
                eq(utcCutoff),
                any(Pageable.class)
        )).thenReturn(List.of());

        int deletedCount = adapter.cleanupExpiredOrRevokedSessions(cutoff, 25);

        assertThat(deletedCount).isZero();
        verify(authSessionRepository, never()).deleteByIdIn(any());
    }

    private AuthSessionEntity authSessionEntity(Long id,
                                                String sessionId,
                                                LocalDateTime createdAt,
                                                LocalDateTime expiresAt,
                                                LocalDateTime revokedAt) {
        AuthSessionEntity entity = new AuthSessionEntity();
        entity.setId(id);
        entity.setSessionId(sessionId);
        entity.setTenantId(1L);
        entity.setUserId(101L);
        entity.setStatus("ACTIVE");
        entity.setCreatedAt(createdAt);
        entity.setExpiresAt(expiresAt);
        entity.setRevokedAt(revokedAt);
        return entity;
    }
}
