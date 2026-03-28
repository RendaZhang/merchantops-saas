package com.renda.merchantops.api.ai.importjob.fixrecommendation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.renda.merchantops.api.ai.client.StructuredOutputAiResponse;
import com.renda.merchantops.api.ai.core.AiProviderException;
import com.renda.merchantops.api.ai.support.StubStructuredOutputAiClient;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenAiImportJobFixRecommendationProviderTest {

    private final StubStructuredOutputAiClient structuredOutputAiClient = new StubStructuredOutputAiClient();
    private final OpenAiImportJobFixRecommendationProvider provider =
            new OpenAiImportJobFixRecommendationProvider(new ObjectMapper(), structuredOutputAiClient);

    @Test
    void generateFixRecommendationShouldBuildStructuredOutputRequestAndParsePayload() {
        structuredOutputAiClient.willReturn(new StructuredOutputAiResponse(
                """
                        {
                          "summary":"row-level failures still point to role validation",
                          "recommendedFixes":[
                            {
                              "errorCode":"UNKNOWN_ROLE",
                              "recommendedAction":"Verify that the referenced role codes exist in the current tenant before replay.",
                              "reasoning":"The grouped failures point to tenant role validation.",
                              "reviewRequired":true
                            },
                            {
                              "errorCode":"DUPLICATE_USERNAME",
                              "recommendedAction":"Review source usernames against current-tenant users before replay.",
                              "reasoning":"The grouped failures point to uniqueness conflicts.",
                              "reviewRequired":true
                            }
                          ],
                          "confidenceNotes":["operators should still confirm tenant-specific business rules"],
                          "recommendedOperatorChecks":["review the affected rows in /errors before replay"]
                        }
                        """,
                "gpt-4.1-mini",
                130,
                48,
                178,
                null
        ));

        ImportJobFixRecommendationProviderResult result = provider.generateFixRecommendation(sampleRequest());

        assertThat(result.summary()).isEqualTo("row-level failures still point to role validation");
        assertThat(result.recommendedFixes()).hasSize(2);
        assertThat(result.recommendedFixes().get(0).errorCode()).isEqualTo("UNKNOWN_ROLE");
        assertThat(result.recommendedFixes().get(1).recommendedAction()).contains("Review source usernames");
        assertThat(result.confidenceNotes()).containsExactly("operators should still confirm tenant-specific business rules");
        assertThat(result.recommendedOperatorChecks()).containsExactly("review the affected rows in /errors before replay");
        assertThat(result.modelId()).isEqualTo("gpt-4.1-mini");
        assertThat(result.totalTokens()).isEqualTo(178);
        assertThat(structuredOutputAiClient.lastRequest().schemaName()).isEqualTo("import_job_fix_recommendation_response");
        assertThat(structuredOutputAiClient.lastRequest().maxOutputTokens()).isEqualTo(520);
        assertThat(structuredOutputAiClient.lastRequest().exampleJson()).contains("\"recommendedFixes\"");
        assertThat(structuredOutputAiClient.lastRequest().exampleJson()).contains("\"UNKNOWN_ROLE\"");
    }

    @Test
    void generateFixRecommendationShouldRejectInvalidJsonPayload() {
        structuredOutputAiClient.willReturn(new StructuredOutputAiResponse(
                "not-json",
                "gpt-4.1-mini",
                130,
                48,
                178,
                null
        ));

        assertThatThrownBy(() -> provider.generateFixRecommendation(sampleRequest()))
                .isInstanceOf(AiProviderException.class)
                .hasMessage("provider import fix recommendation payload is invalid");
    }

    @Test
    void generateFixRecommendationShouldRejectMissingRecommendedFixes() {
        structuredOutputAiClient.willReturn(new StructuredOutputAiResponse(
                """
                        {
                          "summary":"hello",
                          "confidenceNotes":["note one"],
                          "recommendedOperatorChecks":["check one"]
                        }
                        """,
                "gpt-4.1-mini",
                130,
                48,
                178,
                null
        ));

        assertThatThrownBy(() -> provider.generateFixRecommendation(sampleRequest()))
                .isInstanceOf(AiProviderException.class)
                .hasMessage("provider import fix recommendation payload is missing recommendedFixes");
    }

    @Test
    void generateFixRecommendationShouldRejectEmptyRecommendedFixes() {
        structuredOutputAiClient.willReturn(new StructuredOutputAiResponse(
                """
                        {
                          "summary":"hello",
                          "recommendedFixes":[],
                          "confidenceNotes":["note one"],
                          "recommendedOperatorChecks":["check one"]
                        }
                        """,
                "gpt-4.1-mini",
                130,
                48,
                178,
                null
        ));

        assertThatThrownBy(() -> provider.generateFixRecommendation(sampleRequest()))
                .isInstanceOf(AiProviderException.class)
                .hasMessage("provider import fix recommendation payload has empty recommendedFixes");
    }

    @Test
    void generateFixRecommendationShouldRejectBlankRecommendedAction() {
        structuredOutputAiClient.willReturn(new StructuredOutputAiResponse(
                """
                        {
                          "summary":"hello",
                          "recommendedFixes":[
                            {
                              "errorCode":"UNKNOWN_ROLE",
                              "recommendedAction":"   ",
                              "reasoning":"reason",
                              "reviewRequired":true
                            }
                          ],
                          "confidenceNotes":["note one"],
                          "recommendedOperatorChecks":["check one"]
                        }
                        """,
                "gpt-4.1-mini",
                130,
                48,
                178,
                null
        ));

        assertThatThrownBy(() -> provider.generateFixRecommendation(sampleRequest()))
                .isInstanceOf(AiProviderException.class)
                .hasMessage("provider import fix recommendation payload is missing recommendedAction");
    }

    @Test
    void generateFixRecommendationShouldRejectInvalidReviewRequired() {
        structuredOutputAiClient.willReturn(new StructuredOutputAiResponse(
                """
                        {
                          "summary":"hello",
                          "recommendedFixes":[
                            {
                              "errorCode":"UNKNOWN_ROLE",
                              "recommendedAction":"verify tenant roles",
                              "reasoning":"reason",
                              "reviewRequired":"yes"
                            }
                          ],
                          "confidenceNotes":["note one"],
                          "recommendedOperatorChecks":["check one"]
                        }
                        """,
                "gpt-4.1-mini",
                130,
                48,
                178,
                null
        ));

        assertThatThrownBy(() -> provider.generateFixRecommendation(sampleRequest()))
                .isInstanceOf(AiProviderException.class)
                .hasMessage("provider import fix recommendation payload has invalid reviewRequired");
    }

    private ImportJobFixRecommendationProviderRequest sampleRequest() {
        return new ImportJobFixRecommendationProviderRequest(
                "import-ai-fix-recommendation-provider-test-1",
                302L,
                "gpt-4.1-mini",
                1000,
                new ImportJobFixRecommendationPrompt("import-fix-recommendation-v1", "system", "user")
        );
    }
}
