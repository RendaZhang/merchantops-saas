package com.renda.merchantops.api.ai.client;

import com.renda.merchantops.api.config.AiProperties;
import com.renda.merchantops.api.config.AiProviderType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DelegatingStructuredOutputAiClientTest {

    private final OpenAiResponsesStructuredOutputAiClient openAiRawHttpClient =
            mock(OpenAiResponsesStructuredOutputAiClient.class);
    private final SpringAiOpenAiStructuredOutputAiClient openAiSpringAiClient =
            mock(SpringAiOpenAiStructuredOutputAiClient.class);
    private final DeepSeekChatCompletionsStructuredOutputAiClient deepSeekClient =
            mock(DeepSeekChatCompletionsStructuredOutputAiClient.class);
    private final AiProperties aiProperties = new AiProperties();
    private final DelegatingStructuredOutputAiClient client = new DelegatingStructuredOutputAiClient(
            openAiRawHttpClient,
            openAiSpringAiClient,
            deepSeekClient,
            aiProperties
    );

    @Test
    void generateShouldUseRawHttpOpenAiRuntimeByDefault() {
        StructuredOutputAiRequest request = sampleRequest();
        StructuredOutputAiResponse response = new StructuredOutputAiResponse("{}", "gpt-4.1-mini", 1, 2, 3, null);
        aiProperties.setProvider(AiProviderType.OPENAI);
        when(openAiRawHttpClient.generate(request)).thenReturn(response);

        StructuredOutputAiResponse actual = client.generate(request);

        assertThat(actual).isEqualTo(response);
        verify(openAiRawHttpClient).generate(request);
        verifyNoInteractions(openAiSpringAiClient, deepSeekClient);
    }

    @Test
    void generateShouldUseSpringAiOpenAiRuntimeWhenConfigured() {
        StructuredOutputAiRequest request = sampleRequest();
        StructuredOutputAiResponse response = new StructuredOutputAiResponse("{}", "gpt-4.1-mini", 1, 2, 3, null);
        aiProperties.setProvider(AiProviderType.OPENAI);
        aiProperties.setOpenAiRuntime(AiProperties.OpenAiRuntime.SPRING_AI);
        when(openAiSpringAiClient.generate(request)).thenReturn(response);

        StructuredOutputAiResponse actual = client.generate(request);

        assertThat(actual).isEqualTo(response);
        verify(openAiSpringAiClient).generate(request);
        verifyNoInteractions(openAiRawHttpClient, deepSeekClient);
    }

    @Test
    void generateShouldKeepDeepSeekPathIndependentFromOpenAiRuntimeSelector() {
        StructuredOutputAiRequest request = sampleRequest();
        StructuredOutputAiResponse response = new StructuredOutputAiResponse("{}", "deepseek-chat", 1, 2, 3, null);
        aiProperties.setProvider(AiProviderType.DEEPSEEK);
        aiProperties.setOpenAiRuntime(AiProperties.OpenAiRuntime.SPRING_AI);
        when(deepSeekClient.generate(request)).thenReturn(response);

        StructuredOutputAiResponse actual = client.generate(request);

        assertThat(actual).isEqualTo(response);
        verify(deepSeekClient).generate(request);
        verifyNoInteractions(openAiRawHttpClient, openAiSpringAiClient);
    }

    private StructuredOutputAiRequest sampleRequest() {
        return new StructuredOutputAiRequest(
                "req-1",
                "gpt-4.1-mini",
                1000,
                "system",
                "user",
                220,
                "summary_shape",
                java.util.Map.of("type", "object"),
                "{\"summary\":\"example\"}"
        );
    }
}
