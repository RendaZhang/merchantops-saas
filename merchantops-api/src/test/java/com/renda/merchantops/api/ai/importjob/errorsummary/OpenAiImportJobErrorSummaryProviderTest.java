package com.renda.merchantops.api.ai.importjob.errorsummary;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.renda.merchantops.api.ai.client.StructuredOutputAiResponse;
import com.renda.merchantops.api.ai.core.AiProviderException;
import com.renda.merchantops.api.ai.support.StubStructuredOutputAiClient;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenAiImportJobErrorSummaryProviderTest {

    private final StubStructuredOutputAiClient structuredOutputAiClient = new StubStructuredOutputAiClient();
    private final OpenAiImportJobErrorSummaryProvider provider =
            new OpenAiImportJobErrorSummaryProvider(new ObjectMapper(), structuredOutputAiClient);

    @Test
    void generateErrorSummaryShouldBuildStructuredOutputRequestAndParsePayload() {
        structuredOutputAiClient.willReturn(new StructuredOutputAiResponse(
                """
                        {
                          "summary":"hello world",
                          "topErrorPatterns":["pattern one","pattern two"],
                          "recommendedNextSteps":["step one","step two"]
                        }
                        """,
                "gpt-4.1-mini",
                120,
                44,
                164,
                null
        ));

        ImportJobErrorSummaryProviderResult result = provider.generateErrorSummary(sampleRequest());

        assertThat(result.summary()).isEqualTo("hello world");
        assertThat(result.topErrorPatterns()).containsExactly("pattern one", "pattern two");
        assertThat(result.recommendedNextSteps()).containsExactly("step one", "step two");
        assertThat(result.modelId()).isEqualTo("gpt-4.1-mini");
        assertThat(result.totalTokens()).isEqualTo(164);
        assertThat(structuredOutputAiClient.lastRequest().schemaName()).isEqualTo("import_job_error_summary_response");
        assertThat(structuredOutputAiClient.lastRequest().maxOutputTokens()).isEqualTo(360);
        assertThat(structuredOutputAiClient.lastRequest().exampleJson()).contains("\"topErrorPatterns\"");
    }

    @Test
    void generateErrorSummaryShouldRejectInvalidJsonPayload() {
        structuredOutputAiClient.willReturn(new StructuredOutputAiResponse(
                "not-json",
                "gpt-4.1-mini",
                120,
                44,
                164,
                null
        ));

        assertThatThrownBy(() -> provider.generateErrorSummary(sampleRequest()))
                .isInstanceOf(AiProviderException.class)
                .hasMessage("provider import error summary payload is invalid");
    }

    @Test
    void generateErrorSummaryShouldRejectMissingSummary() {
        structuredOutputAiClient.willReturn(new StructuredOutputAiResponse(
                """
                        {
                          "topErrorPatterns":["pattern one"],
                          "recommendedNextSteps":["step one"]
                        }
                        """,
                "gpt-4.1-mini",
                120,
                44,
                164,
                null
        ));

        assertThatThrownBy(() -> provider.generateErrorSummary(sampleRequest()))
                .isInstanceOf(AiProviderException.class)
                .hasMessage("provider import error summary payload is missing summary");
    }

    @Test
    void generateErrorSummaryShouldRejectBlankArrayItem() {
        structuredOutputAiClient.willReturn(new StructuredOutputAiResponse(
                """
                        {
                          "summary":"hello",
                          "topErrorPatterns":["pattern one","   "],
                          "recommendedNextSteps":["step one"]
                        }
                        """,
                "gpt-4.1-mini",
                120,
                44,
                164,
                null
        ));

        assertThatThrownBy(() -> provider.generateErrorSummary(sampleRequest()))
                .isInstanceOf(AiProviderException.class)
                .hasMessage("provider import error summary payload has blank topErrorPatterns item");
    }

    private ImportJobErrorSummaryProviderRequest sampleRequest() {
        return new ImportJobErrorSummaryProviderRequest(
                "import-ai-error-summary-provider-test-1",
                302L,
                "gpt-4.1-mini",
                1000,
                new ImportJobErrorSummaryPrompt("import-error-summary-v1", "system", "user")
        );
    }
}
