package com.renda.merchantops.domain.featureflag;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class FeatureFlagQueryServiceTest {

    @Test
    void listFlagsShouldIncludeKnownDefaultsWhenRowsAreMissing() {
        RecordingFeatureFlagQueryPort port = new RecordingFeatureFlagQueryPort();
        port.listResult = List.of(
                new FeatureFlagItem(
                        8L,
                        7L,
                        "workflow.ticket.comment-proposal.enabled",
                        false,
                        101L,
                        LocalDateTime.of(2026, 4, 8, 8, 0),
                        LocalDateTime.of(2026, 4, 8, 9, 0)
                ),
                new FeatureFlagItem(
                        99L,
                        7L,
                        "unknown.flag.enabled",
                        false,
                        101L,
                        LocalDateTime.of(2026, 4, 8, 8, 0),
                        LocalDateTime.of(2026, 4, 8, 9, 0)
                )
        );
        FeatureFlagQueryService service = new FeatureFlagQueryService(port);

        List<FeatureFlagItem> result = service.listFlags(7L);

        assertThat(result).hasSize(FeatureFlagKey.orderedKeys().size());
        assertThat(result).extracting(FeatureFlagItem::key)
                .containsExactlyElementsOf(FeatureFlagKey.orderedKeys());

        FeatureFlagItem missingSummary = result.stream()
                .filter(item -> item.key().equals("ai.ticket.summary.enabled"))
                .findFirst()
                .orElseThrow();
        assertThat(missingSummary.id()).isNull();
        assertThat(missingSummary.tenantId()).isEqualTo(7L);
        assertThat(missingSummary.enabled()).isTrue();
        assertThat(missingSummary.updatedAt()).isNull();

        FeatureFlagItem storedWorkflowFlag = result.stream()
                .filter(item -> item.key().equals("workflow.ticket.comment-proposal.enabled"))
                .findFirst()
                .orElseThrow();
        assertThat(storedWorkflowFlag.id()).isEqualTo(8L);
        assertThat(storedWorkflowFlag.enabled()).isFalse();
    }

    @Test
    void findByKeyShouldReturnDefaultEnabledItemForKnownMissingFlag() {
        FeatureFlagQueryService service = new FeatureFlagQueryService(new RecordingFeatureFlagQueryPort());

        FeatureFlagItem result = service.findByKey(9L, "ai.ticket.summary.enabled").orElseThrow();

        assertThat(result.id()).isNull();
        assertThat(result.tenantId()).isEqualTo(9L);
        assertThat(result.key()).isEqualTo("ai.ticket.summary.enabled");
        assertThat(result.enabled()).isTrue();
        assertThat(result.updatedBy()).isNull();
        assertThat(result.createdAt()).isNull();
        assertThat(result.updatedAt()).isNull();
    }

    private static final class RecordingFeatureFlagQueryPort implements FeatureFlagQueryPort {

        private List<FeatureFlagItem> listResult = List.of();
        private Optional<FeatureFlagItem> findResult = Optional.empty();

        @Override
        public List<FeatureFlagItem> listFlags(Long tenantId) {
            return listResult;
        }

        @Override
        public Optional<FeatureFlagItem> findByKey(Long tenantId, String key) {
            return findResult;
        }
    }
}
