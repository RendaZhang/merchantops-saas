package com.renda.merchantops.api.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "merchantops.ai")
public class AiProperties {

    private static final String DEFAULT_OPENAI_BASE_URL = "https://api.openai.com";
    private static final String DEFAULT_DEEPSEEK_BASE_URL = "https://api.deepseek.com";
    private static final String DEFAULT_DEEPSEEK_MODEL = "deepseek-chat";

    private boolean enabled = false;

    private String promptVersion = "ticket-summary-v1";

    private String triagePromptVersion = "ticket-triage-v1";

    private String replyDraftPromptVersion = "ticket-reply-draft-v1";

    private String importErrorSummaryPromptVersion = "import-error-summary-v1";

    private String importMappingSuggestionPromptVersion = "import-mapping-suggestion-v1";

    private String importFixRecommendationPromptVersion = "import-fix-recommendation-v1";

    private AiProviderType provider = AiProviderType.OPENAI;

    private String baseUrl;

    private String apiKey;

    private String modelId;

    @Min(100)
    private int timeoutMs = 5000;

    @Valid
    private OpenAiProperties openai = new OpenAiProperties();

    public boolean hasProviderConfiguration() {
        return StringUtils.hasText(resolveModelId())
                && StringUtils.hasText(resolveApiKey())
                && StringUtils.hasText(resolveBaseUrl());
    }

    public AiProviderType resolveProvider() {
        return provider == null ? AiProviderType.OPENAI : provider;
    }

    public String resolveBaseUrl() {
        return switch (resolveProvider()) {
            case OPENAI -> firstNonBlank(baseUrl, openai.getBaseUrl(), DEFAULT_OPENAI_BASE_URL);
            case DEEPSEEK -> firstNonBlank(baseUrl, envLike("DEEPSEEK_BASE_URL"), DEFAULT_DEEPSEEK_BASE_URL);
        };
    }

    public String resolveApiKey() {
        return switch (resolveProvider()) {
            case OPENAI -> firstNonBlank(apiKey, openai.getApiKey());
            case DEEPSEEK -> firstNonBlank(apiKey, envLike("DEEPSEEK_API_KEY"));
        };
    }

    public String resolveModelId() {
        return switch (resolveProvider()) {
            case OPENAI -> normalizeNullable(modelId);
            case DEEPSEEK -> firstNonBlank(modelId, envLike("DEEPSEEK_MODEL"), DEFAULT_DEEPSEEK_MODEL);
        };
    }

    @Data
    public static class OpenAiProperties {

        private String baseUrl;

        private String apiKey;
    }

    private String envLike(String key) {
        return firstNonBlank(System.getProperty(key), System.getenv(key));
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            String normalized = normalizeNullable(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private String normalizeNullable(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
