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

    private final OpenAiResponsesStructuredOutputAiClient openAiRawHttpClient;
    private final SpringAiOpenAiStructuredOutputAiClient openAiSpringAiClient;
    private final DeepSeekChatCompletionsStructuredOutputAiClient deepSeekClient;
    private final AiProperties aiProperties;

    @Override
    public StructuredOutputAiResponse generate(StructuredOutputAiRequest request) {
        AiProviderType provider = aiProperties.resolveProvider();
        return switch (provider) {
            case OPENAI -> switch (aiProperties.resolveOpenAiRuntime()) {
                case RAW_HTTP -> openAiRawHttpClient.generate(request);
                case SPRING_AI -> openAiSpringAiClient.generate(request);
            };
            case DEEPSEEK -> deepSeekClient.generate(request);
        };
    }
}
