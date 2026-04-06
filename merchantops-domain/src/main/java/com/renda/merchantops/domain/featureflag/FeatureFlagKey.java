package com.renda.merchantops.domain.featureflag;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public enum FeatureFlagKey {
    AI_TICKET_SUMMARY("ai.ticket.summary.enabled"),
    AI_TICKET_TRIAGE("ai.ticket.triage.enabled"),
    AI_TICKET_REPLY_DRAFT("ai.ticket.reply-draft.enabled"),
    AI_IMPORT_ERROR_SUMMARY("ai.import.error-summary.enabled"),
    AI_IMPORT_MAPPING_SUGGESTION("ai.import.mapping-suggestion.enabled"),
    AI_IMPORT_FIX_RECOMMENDATION("ai.import.fix-recommendation.enabled"),
    WORKFLOW_IMPORT_SELECTIVE_REPLAY_PROPOSAL("workflow.import.selective-replay-proposal.enabled"),
    WORKFLOW_TICKET_COMMENT_PROPOSAL("workflow.ticket.comment-proposal.enabled");

    private final String key;

    FeatureFlagKey(String key) {
        this.key = key;
    }

    public String key() {
        return key;
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

    public static List<String> orderedKeys() {
        return Arrays.stream(values())
                .map(FeatureFlagKey::key)
                .sorted()
                .toList();
    }

    public static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
