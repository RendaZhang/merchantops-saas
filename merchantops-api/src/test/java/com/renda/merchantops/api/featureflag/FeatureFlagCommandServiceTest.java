package com.renda.merchantops.api.featureflag;

import com.renda.merchantops.api.audit.AuditEventService;
import com.renda.merchantops.api.dto.featureflag.command.FeatureFlagUpdateRequest;
import com.renda.merchantops.domain.featureflag.FeatureFlagCommandUseCase;
import com.renda.merchantops.domain.featureflag.FeatureFlagItem;
import com.renda.merchantops.domain.featureflag.FeatureFlagQueryUseCase;
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
    void updateFlagShouldKeepIdempotentBehaviorForUnchangedEnabledValue() {
        FeatureFlagItem current = flagItem(false);
        when(featureFlagQueryUseCase.findByKey(1L, "ai.ticket.summary.enabled"))
                .thenReturn(Optional.of(current));

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
        verifyNoInteractions(featureFlagCommandUseCase, auditEventService);
    }

    private FeatureFlagItem flagItem(boolean enabled) {
        LocalDateTime createdAt = LocalDateTime.of(2026, 4, 6, 10, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 4, 6, 10, 5);
        return new FeatureFlagItem(
                1L,
                1L,
                "ai.ticket.summary.enabled",
                enabled,
                101L,
                createdAt,
                updatedAt
        );
    }
}
