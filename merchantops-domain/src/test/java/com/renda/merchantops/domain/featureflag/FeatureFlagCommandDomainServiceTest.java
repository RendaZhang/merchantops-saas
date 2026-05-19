package com.renda.merchantops.domain.featureflag;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class FeatureFlagCommandDomainServiceTest {

    @Test
    void updateFlagShouldReturnNoMutationWhenTargetStateAlreadyApplied() {
        RecordingFeatureFlagCommandPort port = new RecordingFeatureFlagCommandPort();
        port.current = managedFlag(true, 101L, LocalDateTime.of(2026, 4, 8, 9, 0));
        FeatureFlagCommandDomainService service = new FeatureFlagCommandDomainService(port);

        FeatureFlagWriteResult result = service.updateFlag(
                1L,
                105L,
                "ai.ticket.summary.enabled",
                new UpdateFeatureFlagCommand(true)
        );

        assertThat(result.mutated()).isFalse();
        assertThat(result.before()).isEqualTo(item(true, 101L, LocalDateTime.of(2026, 4, 8, 9, 0)));
        assertThat(result.after()).isEqualTo(item(true, 101L, LocalDateTime.of(2026, 4, 8, 9, 0)));
        assertThat(port.saveCalls).isZero();
        assertThat(port.savedRequest).isNull();
    }

    @Test
    void updateFlagShouldPersistAndReturnBeforeAfterSnapshotsWhenStateChanges() {
        RecordingFeatureFlagCommandPort port = new RecordingFeatureFlagCommandPort();
        port.current = managedFlag(true, 101L, LocalDateTime.of(2026, 4, 8, 9, 0));
        port.saved = managedFlag(false, 105L, LocalDateTime.of(2026, 4, 8, 9, 30));
        FeatureFlagCommandDomainService service = new FeatureFlagCommandDomainService(port);

        FeatureFlagWriteResult result = service.updateFlag(
                1L,
                105L,
                "ai.ticket.summary.enabled",
                new UpdateFeatureFlagCommand(false)
        );

        assertThat(result.mutated()).isTrue();
        assertThat(result.before()).isEqualTo(item(true, 101L, LocalDateTime.of(2026, 4, 8, 9, 0)));
        assertThat(result.after()).isEqualTo(item(false, 105L, LocalDateTime.of(2026, 4, 8, 9, 30)));
        assertThat(port.saveCalls).isEqualTo(1);
        assertThat(port.savedRequest).isNotNull();
        assertThat(port.savedRequest.enabled()).isFalse();
        assertThat(port.savedRequest.updatedBy()).isEqualTo(105L);
        assertThat(port.savedRequest.createdAt()).isEqualTo(port.current.createdAt());
    }

    @Test
    void updateFlagShouldCreateMissingKnownFlagFromDefaultEnabledState() {
        RecordingFeatureFlagCommandPort port = new RecordingFeatureFlagCommandPort();
        port.saved = new ManagedFeatureFlag(
                9L,
                1L,
                "ai.ticket.summary.enabled",
                false,
                105L,
                LocalDateTime.of(2026, 4, 8, 9, 0),
                LocalDateTime.of(2026, 4, 8, 9, 0)
        );
        FeatureFlagCommandDomainService service = new FeatureFlagCommandDomainService(port);

        FeatureFlagWriteResult result = service.updateFlag(
                1L,
                105L,
                "ai.ticket.summary.enabled",
                new UpdateFeatureFlagCommand(false)
        );

        assertThat(result.mutated()).isTrue();
        assertThat(result.before()).isEqualTo(new FeatureFlagItem(
                null,
                1L,
                "ai.ticket.summary.enabled",
                true,
                null,
                null,
                null
        ));
        assertThat(result.after()).isEqualTo(new FeatureFlagItem(
                9L,
                1L,
                "ai.ticket.summary.enabled",
                false,
                105L,
                LocalDateTime.of(2026, 4, 8, 9, 0),
                LocalDateTime.of(2026, 4, 8, 9, 0)
        ));
        assertThat(port.saveCalls).isEqualTo(1);
        assertThat(port.savedRequest).isNotNull();
        assertThat(port.savedRequest.id()).isNull();
        assertThat(port.savedRequest.enabled()).isFalse();
        assertThat(port.savedRequest.updatedBy()).isEqualTo(105L);
        assertThat(port.savedRequest.createdAt()).isNotNull();
        assertThat(port.savedRequest.updatedAt()).isEqualTo(port.savedRequest.createdAt());
    }

    private static FeatureFlagItem item(boolean enabled, Long updatedBy, LocalDateTime updatedAt) {
        return new FeatureFlagItem(
                1L,
                1L,
                "ai.ticket.summary.enabled",
                enabled,
                updatedBy,
                LocalDateTime.of(2026, 4, 8, 8, 0),
                updatedAt
        );
    }

    private static ManagedFeatureFlag managedFlag(boolean enabled, Long updatedBy, LocalDateTime updatedAt) {
        return new ManagedFeatureFlag(
                1L,
                1L,
                "ai.ticket.summary.enabled",
                enabled,
                updatedBy,
                LocalDateTime.of(2026, 4, 8, 8, 0),
                updatedAt
        );
    }

    private static final class RecordingFeatureFlagCommandPort implements FeatureFlagCommandPort {

        private ManagedFeatureFlag current;
        private ManagedFeatureFlag saved;
        private ManagedFeatureFlag savedRequest;
        private int saveCalls;

        @Override
        public Optional<ManagedFeatureFlag> findByKeyForUpdate(Long tenantId, String key) {
            return Optional.ofNullable(current);
        }

        @Override
        public ManagedFeatureFlag save(ManagedFeatureFlag featureFlag) {
            this.savedRequest = featureFlag;
            this.saveCalls++;
            return saved == null ? featureFlag : saved;
        }
    }
}
