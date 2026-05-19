package com.renda.merchantops.domain.featureflag;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public enum FeatureFlagKey {
    AI_TICKET_SUMMARY("ai.ticket.summary.enabled", true),
    AI_TICKET_TRIAGE("ai.ticket.triage.enabled", true),
    AI_TICKET_REPLY_DRAFT("ai.ticket.reply-draft.enabled", true),
    AI_IMPORT_ERROR_SUMMARY("ai.import.error-summary.enabled", true),
    AI_IMPORT_MAPPING_SUGGESTION("ai.import.mapping-suggestion.enabled", true),
    AI_IMPORT_FIX_RECOMMENDATION("ai.import.fix-recommendation.enabled", true),
    WORKFLOW_IMPORT_SELECTIVE_REPLAY_PROPOSAL("workflow.import.selective-replay-proposal.enabled", true),
    WORKFLOW_TICKET_COMMENT_PROPOSAL("workflow.ticket.comment-proposal.enabled", true);

    private final String key;
    private final boolean defaultEnabled;

    FeatureFlagKey(String key, boolean defaultEnabled) {
        this.key = key;
        this.defaultEnabled = defaultEnabled;
    }

    public String key() {
        return key;
    }

    public boolean defaultEnabled() {
        return defaultEnabled;
    }

    public FeatureFlagItem defaultItem(Long tenantId) {
        return new FeatureFlagItem(
                null,
                tenantId,
                key,
                defaultEnabled,
                null,
                null,
                null
        );
    }

    public ManagedFeatureFlag defaultManagedFlag(Long tenantId) {
        return new ManagedFeatureFlag(
                null,
                tenantId,
                key,
                defaultEnabled,
                null,
                null,
                null
        );
    }

    public static Optional<FeatureFlagKey> fromKey(String key) {
        String normalized = normalize(key);
        if (normalized == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(item -> item.key.equals(normalized))
                .findFirst();
    }

    public static List<FeatureFlagKey> orderedValues() {
        return Arrays.stream(values())
                .sorted((left, right) -> left.key.compareTo(right.key))
                .toList();
    }

    public static List<String> orderedKeys() {
        return orderedValues().stream()
                .map(FeatureFlagKey::key)
                .toList();
    }

    public static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
