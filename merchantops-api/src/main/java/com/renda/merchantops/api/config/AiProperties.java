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

    private boolean enabled = false;

    private String promptVersion = "ticket-summary-v1";

    private String triagePromptVersion = "ticket-triage-v1";

    private String replyDraftPromptVersion = "ticket-reply-draft-v1";

    private String modelId;

    @Min(100)
    private int timeoutMs = 5000;

    @Valid
    private OpenAiProperties openai = new OpenAiProperties();

    public boolean hasProviderConfiguration() {
        return StringUtils.hasText(modelId)
                && StringUtils.hasText(openai.getApiKey())
                && StringUtils.hasText(openai.getBaseUrl());
    }

    @Data
    public static class OpenAiProperties {

        private String baseUrl = "https://api.openai.com";

        private String apiKey;
    }
}
