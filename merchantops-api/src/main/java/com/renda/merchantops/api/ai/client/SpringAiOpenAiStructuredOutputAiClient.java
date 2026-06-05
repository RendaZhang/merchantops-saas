package com.renda.merchantops.api.ai.client;

import com.renda.merchantops.api.ai.core.AiProviderException;
import com.renda.merchantops.api.ai.core.AiProviderFailureType;
import com.renda.merchantops.api.config.AiProperties;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.ai.openai.api.common.OpenAiApiClientErrorException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.retry.support.RetryTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SpringAiOpenAiStructuredOutputAiClient implements StructuredOutputAiClient {

    private static final String OPENAI_CHAT_COMPLETIONS_PATH = "/v1/chat/completions";

    private final RestClient.Builder restClientBuilder;
    private final AiProperties aiProperties;
    private final RetryTemplate aiProviderRetryTemplate;
    private final ToolCallingManager aiProviderToolCallingManager;

    @Override
    public StructuredOutputAiResponse generate(StructuredOutputAiRequest request) {
        try {
            ChatResponse response = buildChatModel(request).call(buildPrompt(request));
            return parseResponse(request, response);
        } catch (AiProviderException ex) {
            throw ex;
        } catch (OpenAiApiClientErrorException ex) {
            throw mapFailure(ex);
        } catch (RestClientResponseException ex) {
            throw mapHttpFailure(ex);
        } catch (ResourceAccessException ex) {
            if (AiProviderHttpSupport.isTimeout(ex)) {
                throw new AiProviderException(AiProviderFailureType.TIMEOUT, "ai provider timed out");
            }
            throw new AiProviderException(AiProviderFailureType.UNAVAILABLE, "ai provider is unreachable");
        } catch (RestClientException ex) {
            throw new AiProviderException(AiProviderFailureType.UNAVAILABLE, "ai provider invocation failed");
        } catch (RuntimeException ex) {
            if (AiProviderHttpSupport.isTimeout(ex)) {
                throw new AiProviderException(AiProviderFailureType.TIMEOUT, "ai provider timed out");
            }
            throw new AiProviderException(AiProviderFailureType.UNAVAILABLE, "ai provider invocation failed");
        }
    }

    private Prompt buildPrompt(StructuredOutputAiRequest request) {
        return new Prompt(List.of(
                new SystemMessage(request.systemPrompt()),
                new UserMessage(request.userPrompt())
        ));
    }

    private OpenAiChatModel buildChatModel(StructuredOutputAiRequest request) {
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(aiProperties.resolveBaseUrl())
                .apiKey(aiProperties.resolveApiKey())
                .completionsPath(OPENAI_CHAT_COMPLETIONS_PATH)
                .restClientBuilder(AiProviderHttpSupport.buildClientBuilder(
                        restClientBuilder,
                        aiProperties.resolveBaseUrl(),
                        request.timeoutMs()
                ))
                .responseErrorHandler(new DefaultResponseErrorHandler())
                .build();

        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(buildOptions(request))
                .toolCallingManager(aiProviderToolCallingManager)
                .retryTemplate(aiProviderRetryTemplate)
                .observationRegistry(ObservationRegistry.NOOP)
                .build();
    }

    private OpenAiChatOptions buildOptions(StructuredOutputAiRequest request) {
        ResponseFormat.JsonSchema jsonSchema = ResponseFormat.JsonSchema.builder()
                .name(request.schemaName())
                .schema(request.schema())
                .strict(true)
                .build();

        Map<String, String> httpHeaders = new LinkedHashMap<>();
        if (StringUtils.hasText(request.requestId())) {
            httpHeaders.put("X-Client-Request-Id", request.requestId());
        }

        return OpenAiChatOptions.builder()
                .model(request.modelId())
                .maxTokens(request.maxOutputTokens())
                .responseFormat(ResponseFormat.builder()
                        .type(ResponseFormat.Type.JSON_SCHEMA)
                        .jsonSchema(jsonSchema)
                        .build())
                .httpHeaders(httpHeaders)
                .build();
    }

    private StructuredOutputAiResponse parseResponse(StructuredOutputAiRequest request, ChatResponse response) {
        if (response == null) {
            throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider response is empty");
        }

        Generation generation = response.getResult();
        if (generation == null) {
            throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider response did not include content");
        }

        AssistantMessage output = generation.getOutput();
        String rawText = output == null ? null : output.getText();
        if (!StringUtils.hasText(rawText)) {
            throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider returned blank content");
        }

        ChatResponseMetadata metadata = response.getMetadata();
        Usage usage = metadata == null ? null : metadata.getUsage();
        Integer inputTokens = usage == null ? null : usage.getPromptTokens();
        Integer outputTokens = usage == null ? null : usage.getCompletionTokens();
        Integer totalTokens = usage == null ? null : usage.getTotalTokens();
        String resolvedModelId = metadata == null ? null : metadata.getModel();

        return new StructuredOutputAiResponse(
                rawText,
                StringUtils.hasText(resolvedModelId) ? resolvedModelId.trim() : request.modelId(),
                inputTokens,
                outputTokens,
                totalTokens,
                null
        );
    }

    private AiProviderException mapFailure(Throwable throwable) {
        RestClientResponseException httpFailure =
                AiProviderHttpSupport.findCause(throwable, RestClientResponseException.class);
        if (httpFailure != null) {
            return mapHttpFailure(httpFailure);
        }
        if (AiProviderHttpSupport.isTimeout(throwable)) {
            return new AiProviderException(AiProviderFailureType.TIMEOUT, "ai provider timed out");
        }
        return new AiProviderException(AiProviderFailureType.UNAVAILABLE, "ai provider invocation failed");
    }

    private AiProviderException mapHttpFailure(RestClientResponseException ex) {
        AiProviderFailureType failureType = AiProviderHttpSupport.classifyHttpFailure(ex);
        if (failureType == AiProviderFailureType.TIMEOUT) {
            return new AiProviderException(AiProviderFailureType.TIMEOUT, "ai provider timed out");
        }
        return new AiProviderException(AiProviderFailureType.UNAVAILABLE, "ai provider returned an error");
    }
}
