package com.renda.merchantops.api.ai.eval;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.renda.merchantops.api.ai.client.OpenAiTestResponseSupport;
import com.renda.merchantops.api.ai.client.StructuredOutputAiResponse;
import com.renda.merchantops.api.ai.core.AiGenerationWorkflow;
import com.renda.merchantops.api.config.AiProperties;
import com.renda.merchantops.api.config.AiProviderType;
import org.assertj.core.api.Assertions;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

abstract class AbstractAiWorkflowEvalRunner implements AiWorkflowEvalRunner {

    protected final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    protected <T> List<T> loadList(String resourcePath, TypeReference<List<T>> typeReference) throws Exception {
        try (InputStream inputStream = getClass().getResourceAsStream(resourcePath)) {
            Assertions.assertThat(inputStream)
                    .as("resource %s should exist", resourcePath)
                    .isNotNull();
            return objectMapper.readValue(inputStream, typeReference);
        }
    }

    protected StructuredOutputAiResponse loadStructuredOutputResponse(String resourcePath) throws Exception {
        try (InputStream inputStream = getClass().getResourceAsStream(resourcePath)) {
            Assertions.assertThat(inputStream)
                    .as("resource %s should exist", resourcePath)
                    .isNotNull();
            JsonNode root = objectMapper.readTree(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
            JsonNode usage = root.path("usage");
            return new StructuredOutputAiResponse(
                    OpenAiTestResponseSupport.extractOutputText(root),
                    root.path("model").asText("gpt-4.1-mini"),
                    usage.hasNonNull("input_tokens") ? usage.get("input_tokens").asInt() : null,
                    usage.hasNonNull("output_tokens") ? usage.get("output_tokens").asInt() : null,
                    usage.hasNonNull("total_tokens") ? usage.get("total_tokens").asInt() : null,
                    null
            );
        }
    }

    protected AiProperties aiProperties(AiGenerationWorkflow workflow) {
        AiProperties aiProperties = new AiProperties();
        aiProperties.setEnabled(true);
        aiProperties.setProvider(AiProviderType.OPENAI);
        workflow.applyPromptVersion(aiProperties, workflow.defaultPromptVersion());
        aiProperties.setModelId("gpt-4.1-mini");
        aiProperties.setTimeoutMs(1000);
        aiProperties.setApiKey("test-key");
        aiProperties.setBaseUrl("https://api.openai.com");
        return aiProperties;
    }

    protected void runSample(List<AiWorkflowEvalFailure> failures,
                             AiWorkflowEvalInventoryEntry inventoryEntry,
                             AiWorkflowEvalSampleType sampleType,
                             String sampleId,
                             ThrowingRunnable runnable) {
        try {
            runnable.run();
        } catch (Throwable throwable) {
            failures.add(new AiWorkflowEvalFailure(
                    inventoryEntry.workflow().workflowName(),
                    sampleType,
                    sampleId,
                    rootMessage(throwable)
            ));
        }
    }

    protected AiWorkflowEvalResult result(AiWorkflowEvalInventoryEntry inventoryEntry,
                                          int goldenCount,
                                          int failureCount,
                                          int policyCount,
                                          List<AiWorkflowEvalFailure> failures) {
        return new AiWorkflowEvalResult(inventoryEntry, goldenCount, failureCount, policyCount, List.copyOf(failures));
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null || message.isBlank() ? current.getClass().getSimpleName() : message;
    }

    @FunctionalInterface
    protected interface ThrowingRunnable {
        void run() throws Exception;
    }
}
