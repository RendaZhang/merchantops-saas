package com.renda.merchantops.api.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiPropertiesTest {

    @AfterEach
    void tearDown() {
        System.clearProperty("DEEPSEEK_API_KEY");
        System.clearProperty("DEEPSEEK_BASE_URL");
        System.clearProperty("DEEPSEEK_MODEL");
    }

    @Test
    void shouldResolveOpenAiFormalKeysBeforeLegacyKeys() {
        AiProperties properties = new AiProperties();
        properties.setProvider(AiProviderType.OPENAI);
        properties.setBaseUrl("https://custom-openai.local");
        properties.setApiKey("formal-key");
        properties.setModelId("gpt-4.1-mini");
        properties.getOpenai().setBaseUrl("https://legacy-openai.local");
        properties.getOpenai().setApiKey("legacy-key");

        assertThat(properties.resolveBaseUrl()).isEqualTo("https://custom-openai.local");
        assertThat(properties.resolveApiKey()).isEqualTo("formal-key");
        assertThat(properties.resolveModelId()).isEqualTo("gpt-4.1-mini");
        assertThat(properties.hasProviderConfiguration()).isTrue();
    }

    @Test
    void shouldResolveDeepSeekAliasAndDefaults() {
        System.setProperty("DEEPSEEK_API_KEY", "deepseek-key");

        AiProperties properties = new AiProperties();
        properties.setProvider(AiProviderType.DEEPSEEK);

        assertThat(properties.resolveBaseUrl()).isEqualTo("https://api.deepseek.com");
        assertThat(properties.resolveApiKey()).isEqualTo("deepseek-key");
        assertThat(properties.resolveModelId()).isEqualTo("deepseek-chat");
        assertThat(properties.hasProviderConfiguration()).isTrue();
    }

    @Test
    void shouldResolveDeepSeekFormalKeysBeforeAliases() {
        System.setProperty("DEEPSEEK_API_KEY", "deepseek-key");
        System.setProperty("DEEPSEEK_BASE_URL", "https://alias.deepseek.local");
        System.setProperty("DEEPSEEK_MODEL", "deepseek-reasoner");

        AiProperties properties = new AiProperties();
        properties.setProvider(AiProviderType.DEEPSEEK);
        properties.setBaseUrl("https://formal.deepseek.local");
        properties.setApiKey("formal-key");
        properties.setModelId("deepseek-chat");

        assertThat(properties.resolveBaseUrl()).isEqualTo("https://formal.deepseek.local");
        assertThat(properties.resolveApiKey()).isEqualTo("formal-key");
        assertThat(properties.resolveModelId()).isEqualTo("deepseek-chat");
    }

    @Test
    void shouldFallbackToLegacyOpenAiKeysWhenFormalKeysAreBlank() {
        AiProperties properties = new AiProperties();
        properties.setProvider(AiProviderType.OPENAI);
        properties.setModelId("gpt-4.1-mini");
        properties.getOpenai().setBaseUrl("https://legacy-openai.local");
        properties.getOpenai().setApiKey("legacy-key");

        assertThat(properties.resolveBaseUrl()).isEqualTo("https://legacy-openai.local");
        assertThat(properties.resolveApiKey()).isEqualTo("legacy-key");
        assertThat(properties.hasProviderConfiguration()).isTrue();
    }

    @Test
    void shouldTreatDeepSeekAsNotConfiguredWhenApiKeyIsMissing() {
        AiProperties properties = new AiProperties();
        properties.setProvider(AiProviderType.DEEPSEEK);

        assertThat(properties.resolveBaseUrl()).isEqualTo("https://api.deepseek.com");
        assertThat(properties.resolveModelId()).isEqualTo("deepseek-chat");
        assertThat(properties.resolveApiKey()).isNull();
        assertThat(properties.hasProviderConfiguration()).isFalse();
    }

    @Test
    void shouldDefaultTimeoutToFifteenSeconds() {
        AiProperties properties = new AiProperties();

        assertThat(properties.getTimeoutMs()).isEqualTo(15000);
    }

    @Test
    void shouldDefaultOpenAiRuntimeToRawHttp() {
        AiProperties properties = new AiProperties();

        assertThat(properties.resolveOpenAiRuntime()).isEqualTo(AiProperties.OpenAiRuntime.RAW_HTTP);
    }
}
