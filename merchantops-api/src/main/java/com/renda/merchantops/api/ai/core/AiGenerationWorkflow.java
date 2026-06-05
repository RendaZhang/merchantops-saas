package com.renda.merchantops.api.ai.core;

import com.renda.merchantops.api.config.AiProperties;

public enum AiGenerationWorkflow {
    TICKET_SUMMARY("ticket-summary", "TICKET", "SUMMARY", "ticket-summary-v1") {
        @Override
        protected String configuredPromptVersion(AiProperties aiProperties) {
            return aiProperties.getPromptVersion();
        }

        @Override
        public void applyPromptVersion(AiProperties aiProperties, String promptVersion) {
            aiProperties.setPromptVersion(promptVersion);
        }
    },
    TICKET_TRIAGE("ticket-triage", "TICKET", "TRIAGE", "ticket-triage-v1") {
        @Override
        protected String configuredPromptVersion(AiProperties aiProperties) {
            return aiProperties.getTriagePromptVersion();
        }

        @Override
        public void applyPromptVersion(AiProperties aiProperties, String promptVersion) {
            aiProperties.setTriagePromptVersion(promptVersion);
        }
    },
    TICKET_REPLY_DRAFT("ticket-reply-draft", "TICKET", "REPLY_DRAFT", "ticket-reply-draft-v1") {
        @Override
        protected String configuredPromptVersion(AiProperties aiProperties) {
            return aiProperties.getReplyDraftPromptVersion();
        }

        @Override
        public void applyPromptVersion(AiProperties aiProperties, String promptVersion) {
            aiProperties.setReplyDraftPromptVersion(promptVersion);
        }
    },
    IMPORT_ERROR_SUMMARY("import-job-error-summary", "IMPORT_JOB", "ERROR_SUMMARY", "import-error-summary-v1") {
        @Override
        protected String configuredPromptVersion(AiProperties aiProperties) {
            return aiProperties.getImportErrorSummaryPromptVersion();
        }

        @Override
        public void applyPromptVersion(AiProperties aiProperties, String promptVersion) {
            aiProperties.setImportErrorSummaryPromptVersion(promptVersion);
        }
    },
    IMPORT_MAPPING_SUGGESTION("import-job-mapping-suggestion", "IMPORT_JOB", "MAPPING_SUGGESTION",
            "import-mapping-suggestion-v1") {
        @Override
        protected String configuredPromptVersion(AiProperties aiProperties) {
            return aiProperties.getImportMappingSuggestionPromptVersion();
        }

        @Override
        public void applyPromptVersion(AiProperties aiProperties, String promptVersion) {
            aiProperties.setImportMappingSuggestionPromptVersion(promptVersion);
        }
    },
    IMPORT_FIX_RECOMMENDATION("import-job-fix-recommendation", "IMPORT_JOB", "FIX_RECOMMENDATION",
            "import-fix-recommendation-v1") {
        @Override
        protected String configuredPromptVersion(AiProperties aiProperties) {
            return aiProperties.getImportFixRecommendationPromptVersion();
        }

        @Override
        public void applyPromptVersion(AiProperties aiProperties, String promptVersion) {
            aiProperties.setImportFixRecommendationPromptVersion(promptVersion);
        }
    };

    private final String workflowName;
    private final String entityType;
    private final String interactionType;
    private final String defaultPromptVersion;

    AiGenerationWorkflow(String workflowName, String entityType, String interactionType, String defaultPromptVersion) {
        this.workflowName = workflowName;
        this.entityType = entityType;
        this.interactionType = interactionType;
        this.defaultPromptVersion = defaultPromptVersion;
    }

    public String workflowName() {
        return workflowName;
    }

    public String entityType() {
        return entityType;
    }

    public String interactionType() {
        return interactionType;
    }

    public String defaultPromptVersion() {
        return defaultPromptVersion;
    }

    public String resolvePromptVersion(AiProperties aiProperties, AiInteractionExecutionSupport executionSupport) {
        return executionSupport.normalizePromptVersion(configuredPromptVersion(aiProperties), defaultPromptVersion);
    }

    public abstract void applyPromptVersion(AiProperties aiProperties, String promptVersion);

    protected abstract String configuredPromptVersion(AiProperties aiProperties);
}
