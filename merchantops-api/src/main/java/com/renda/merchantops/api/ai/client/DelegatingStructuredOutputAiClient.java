package com.renda.merchantops.api.ai.client;

import com.renda.merchantops.api.config.AiProperties;
import com.renda.merchantops.api.config.AiProviderType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
@RequiredArgsConstructor
public class DelegatingStructuredOutputAiClient implements StructuredOutputAiClient {

    private final OpenAiResponsesStructuredOutputAiClient openAiClient;
    private final DeepSeekChatCompletionsStructuredOutputAiClient deepSeekClient;
    private final AiProperties aiProperties;

    @Override
    public StructuredOutputAiResponse generate(StructuredOutputAiRequest request) {
        AiProviderType provider = aiProperties.resolveProvider();
        return switch (provider) {
            case OPENAI -> openAiClient.generate(request);
            case DEEPSEEK -> deepSeekClient.generate(request);
        };
    }
}
