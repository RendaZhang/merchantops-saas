package com.renda.merchantops.api.featureflag;

import com.renda.merchantops.api.audit.AuditEventService;
import com.renda.merchantops.api.dto.featureflag.command.FeatureFlagUpdateRequest;
import com.renda.merchantops.domain.featureflag.FeatureFlagCommandUseCase;
import com.renda.merchantops.domain.featureflag.FeatureFlagItem;
import com.renda.merchantops.domain.featureflag.FeatureFlagQueryUseCase;
import com.renda.merchantops.domain.featureflag.FeatureFlagWriteResult;
import com.renda.merchantops.domain.shared.error.BizException;
import com.renda.merchantops.domain.shared.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeatureFlagCommandServiceTest {

    @Mock
    private FeatureFlagQueryUseCase featureFlagQueryUseCase;

    @Mock
    private FeatureFlagCommandUseCase featureFlagCommandUseCase;

    @Mock
    private AuditEventService auditEventService;

    private FeatureFlagCommandService featureFlagCommandService;

    @BeforeEach
    void setUp() {
        featureFlagCommandService = new FeatureFlagCommandService(
                featureFlagQueryUseCase,
                featureFlagCommandUseCase,
                new FeatureFlagQueryService(featureFlagQueryUseCase),
                auditEventService
        );
    }

    @Test
    void updateFlagShouldRejectNullEnabledBeforeIdempotentShortCircuit() {
        FeatureFlagItem current = flagItem(false);
        when(featureFlagQueryUseCase.findByKey(1L, "ai.ticket.summary.enabled"))
                .thenReturn(Optional.of(current));

        assertThatThrownBy(() -> featureFlagCommandService.updateFlag(
                1L,
                101L,
                "feature-flag-null-enabled-1",
                "ai.ticket.summary.enabled",
                new FeatureFlagUpdateRequest(null)
        ))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> assertThat(((BizException) ex).getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST))
                .hasMessage("enabled must not be null");

        verify(featureFlagQueryUseCase).findByKey(1L, "ai.ticket.summary.enabled");
        verifyNoInteractions(featureFlagCommandUseCase, auditEventService);
    }

    @Test
    void updateFlagShouldDelegateIdempotentDecisionToDomainWhenEnabledValueIsUnchanged() {
        FeatureFlagItem current = flagItem(false);
        when(featureFlagQueryUseCase.findByKey(1L, "ai.ticket.summary.enabled"))
                .thenReturn(Optional.of(current));
        when(featureFlagCommandUseCase.updateFlag(eq(1L), eq(101L), eq("ai.ticket.summary.enabled"), any()))
                .thenReturn(FeatureFlagWriteResult.noChange(current));

        var response = featureFlagCommandService.updateFlag(
                1L,
                101L,
                "feature-flag-idempotent-1",
                "ai.ticket.summary.enabled",
                new FeatureFlagUpdateRequest(false)
        );

        assertThat(response.key()).isEqualTo("ai.ticket.summary.enabled");
        assertThat(response.enabled()).isFalse();
        assertThat(response.updatedAt()).isEqualTo(current.updatedAt());
        verify(featureFlagQueryUseCase).findByKey(1L, "ai.ticket.summary.enabled");
        verify(featureFlagCommandUseCase).updateFlag(eq(1L), eq(101L), eq("ai.ticket.summary.enabled"), any());
        verifyNoInteractions(auditEventService);
    }

    @Test
    void updateFlagShouldNotShortCircuitWhenUnlockedReadAlreadyMatchesRequestedValue() {
        FeatureFlagItem staleRead = flagItem(true, 101L, LocalDateTime.of(2026, 4, 8, 10, 0));
        FeatureFlagItem lockedBefore = flagItem(false, 202L, LocalDateTime.of(2026, 4, 8, 10, 15));
        FeatureFlagItem saved = flagItem(true, 101L, LocalDateTime.of(2026, 4, 8, 10, 30));
        when(featureFlagQueryUseCase.findByKey(1L, "ai.ticket.summary.enabled"))
                .thenReturn(Optional.of(staleRead));
        when(featureFlagCommandUseCase.updateFlag(eq(1L), eq(101L), eq("ai.ticket.summary.enabled"), any()))
                .thenReturn(FeatureFlagWriteResult.mutated(lockedBefore, saved));

        var response = featureFlagCommandService.updateFlag(
                1L,
                101L,
                "feature-flag-stale-read-1",
                "ai.ticket.summary.enabled",
                new FeatureFlagUpdateRequest(true)
        );

        assertThat(response.id()).isEqualTo(saved.id());
        assertThat(response.key()).isEqualTo("ai.ticket.summary.enabled");
        assertThat(response.enabled()).isTrue();
        assertThat(response.updatedAt()).isEqualTo(saved.updatedAt());
        verify(featureFlagQueryUseCase).findByKey(1L, "ai.ticket.summary.enabled");
        verify(featureFlagCommandUseCase).updateFlag(eq(1L), eq(101L), eq("ai.ticket.summary.enabled"), any());
        verify(auditEventService).recordEvent(
                eq(1L),
                eq("FEATURE_FLAG"),
                eq(saved.id()),
                eq("FEATURE_FLAG_UPDATED"),
                eq(101L),
                eq("feature-flag-stale-read-1"),
                any(),
                any()
        );
    }

    @Test
    void updateFlagShouldUseDomainAfterStateAndSkipAuditWhenDomainReportsNoMutation() {
        FeatureFlagItem initialRead = flagItem(false);
        FeatureFlagItem concurrentState = flagItem(
                true,
                202L,
                LocalDateTime.of(2026, 4, 8, 11, 30)
        );
        when(featureFlagQueryUseCase.findByKey(1L, "ai.ticket.summary.enabled"))
                .thenReturn(Optional.of(initialRead));
        when(featureFlagCommandUseCase.updateFlag(eq(1L), eq(101L), eq("ai.ticket.summary.enabled"), any()))
                .thenReturn(FeatureFlagWriteResult.noChange(concurrentState));

        var response = featureFlagCommandService.updateFlag(
                1L,
                101L,
                "feature-flag-race-no-mutation-1",
                "ai.ticket.summary.enabled",
                new FeatureFlagUpdateRequest(true)
        );

        assertThat(response.key()).isEqualTo("ai.ticket.summary.enabled");
        assertThat(response.enabled()).isTrue();
        assertThat(response.updatedAt()).isEqualTo(concurrentState.updatedAt());
        verify(featureFlagQueryUseCase).findByKey(1L, "ai.ticket.summary.enabled");
        verify(featureFlagCommandUseCase).updateFlag(eq(1L), eq(101L), eq("ai.ticket.summary.enabled"), any());
        verifyNoInteractions(auditEventService);
    }

    private FeatureFlagItem flagItem(boolean enabled) {
        return flagItem(enabled, 101L, LocalDateTime.of(2026, 4, 6, 10, 5));
    }

    private FeatureFlagItem flagItem(boolean enabled, Long updatedBy, LocalDateTime updatedAt) {
        LocalDateTime createdAt = LocalDateTime.of(2026, 4, 6, 10, 0);
        return new FeatureFlagItem(
                1L,
                1L,
                "ai.ticket.summary.enabled",
                enabled,
                updatedBy,
                createdAt,
                updatedAt
        );
    }
}
