package com.renda.merchantops.api.ai.importjob.mappingsuggestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.renda.merchantops.api.ai.client.StructuredOutputAiResponse;
import com.renda.merchantops.api.ai.core.AiProviderException;
import com.renda.merchantops.api.ai.support.StubStructuredOutputAiClient;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenAiImportJobMappingSuggestionProviderTest {

    private final StubStructuredOutputAiClient structuredOutputAiClient = new StubStructuredOutputAiClient();
    private final OpenAiImportJobMappingSuggestionProvider provider =
            new OpenAiImportJobMappingSuggestionProvider(new ObjectMapper(), structuredOutputAiClient);

    @Test
    void generateMappingSuggestionShouldBuildStructuredOutputRequestAndParsePayload() {
        structuredOutputAiClient.willReturn(new StructuredOutputAiResponse(
                """
                        {
                          "summary":"header still looks close to USER_CSV",
                          "suggestedFieldMappings":[
                            {
                              "canonicalField":"username",
                              "observedColumnSignal":{"headerName":"login","headerPosition":1},
                              "reasoning":"login matches username",
                              "reviewRequired":false
                            },
                            {
                              "canonicalField":"displayName",
                              "observedColumnSignal":{"headerName":"display_name","headerPosition":2},
                              "reasoning":"display_name matches displayName",
                              "reviewRequired":false
                            },
                            {
                              "canonicalField":"email",
                              "observedColumnSignal":{"headerName":"email_address","headerPosition":3},
                              "reasoning":"email_address matches email",
                              "reviewRequired":false
                            },
                            {
                              "canonicalField":"password",
                              "observedColumnSignal":{"headerName":"passwd","headerPosition":4},
                              "reasoning":"passwd should be manually confirmed",
                              "reviewRequired":true
                            },
                            {
                              "canonicalField":"roleCodes",
                              "observedColumnSignal":{"headerName":"roles","headerPosition":5},
                              "reasoning":"roles is the strongest roleCodes signal",
                              "reviewRequired":true
                            }
                          ],
                          "confidenceNotes":["review the failed header before reuse"],
                          "recommendedOperatorChecks":["confirm the source header order"]
                        }
                        """,
                "gpt-4.1-mini",
                120,
                44,
                164,
                null
        ));

        ImportJobMappingSuggestionProviderResult result = provider.generateMappingSuggestion(sampleRequest());

        assertThat(result.summary()).isEqualTo("header still looks close to USER_CSV");
        assertThat(result.suggestedFieldMappings()).hasSize(5);
        assertThat(result.suggestedFieldMappings().get(0).canonicalField()).isEqualTo("username");
        assertThat(result.suggestedFieldMappings().get(0).observedColumnSignal().headerName()).isEqualTo("login");
        assertThat(result.confidenceNotes()).containsExactly("review the failed header before reuse");
        assertThat(result.recommendedOperatorChecks()).containsExactly("confirm the source header order");
        assertThat(result.modelId()).isEqualTo("gpt-4.1-mini");
        assertThat(result.totalTokens()).isEqualTo(164);
        assertThat(structuredOutputAiClient.lastRequest().schemaName()).isEqualTo("import_job_mapping_suggestion_response");
        assertThat(structuredOutputAiClient.lastRequest().maxOutputTokens()).isEqualTo(520);
        assertThat(structuredOutputAiClient.lastRequest().exampleJson()).contains("\"suggestedFieldMappings\"");
        assertThat(structuredOutputAiClient.lastRequest().exampleJson()).contains("\"passwd\"");
    }

    @Test
    void generateMappingSuggestionShouldRejectInvalidJsonPayload() {
        structuredOutputAiClient.willReturn(new StructuredOutputAiResponse(
                "not-json",
                "gpt-4.1-mini",
                120,
                44,
                164,
                null
        ));

        assertThatThrownBy(() -> provider.generateMappingSuggestion(sampleRequest()))
                .isInstanceOf(AiProviderException.class)
                .hasMessage("provider import mapping suggestion payload is invalid");
    }

    @Test
    void generateMappingSuggestionShouldRejectMissingSuggestedFieldMappings() {
        structuredOutputAiClient.willReturn(new StructuredOutputAiResponse(
                """
                        {
                          "summary":"hello",
                          "confidenceNotes":["note one"],
                          "recommendedOperatorChecks":["check one"]
                        }
                        """,
                "gpt-4.1-mini",
                120,
                44,
                164,
                null
        ));

        assertThatThrownBy(() -> provider.generateMappingSuggestion(sampleRequest()))
                .isInstanceOf(AiProviderException.class)
                .hasMessage("provider import mapping suggestion payload is missing suggestedFieldMappings");
    }

    @Test
    void generateMappingSuggestionShouldRejectEmptySuggestedFieldMappings() {
        structuredOutputAiClient.willReturn(new StructuredOutputAiResponse(
                """
                        {
                          "summary":"hello",
                          "suggestedFieldMappings":[],
                          "confidenceNotes":["note one"],
                          "recommendedOperatorChecks":["check one"]
                        }
                        """,
                "gpt-4.1-mini",
                120,
                44,
                164,
                null
        ));

        assertThatThrownBy(() -> provider.generateMappingSuggestion(sampleRequest()))
                .isInstanceOf(AiProviderException.class)
                .hasMessage("provider import mapping suggestion payload has empty suggestedFieldMappings");
    }

    @Test
    void generateMappingSuggestionShouldRejectBlankReasoning() {
        structuredOutputAiClient.willReturn(new StructuredOutputAiResponse(
                """
                        {
                          "summary":"hello",
                          "suggestedFieldMappings":[
                            {
                              "canonicalField":"username",
                              "observedColumnSignal":{"headerName":"login","headerPosition":1},
                              "reasoning":"   ",
                              "reviewRequired":false
                            }
                          ],
                          "confidenceNotes":["note one"],
                          "recommendedOperatorChecks":["check one"]
                        }
                        """,
                "gpt-4.1-mini",
                120,
                44,
                164,
                null
        ));

        assertThatThrownBy(() -> provider.generateMappingSuggestion(sampleRequest()))
                .isInstanceOf(AiProviderException.class)
                .hasMessage("provider import mapping suggestion payload is missing reasoning");
    }

    @Test
    void generateMappingSuggestionShouldRejectInvalidHeaderPosition() {
        structuredOutputAiClient.willReturn(new StructuredOutputAiResponse(
                """
                        {
                          "summary":"hello",
                          "suggestedFieldMappings":[
                            {
                              "canonicalField":"username",
                              "observedColumnSignal":{"headerName":"login","headerPosition":0},
                              "reasoning":"login matches username",
                              "reviewRequired":false
                            }
                          ],
                          "confidenceNotes":["note one"],
                          "recommendedOperatorChecks":["check one"]
                        }
                        """,
                "gpt-4.1-mini",
                120,
                44,
                164,
                null
        ));

        assertThatThrownBy(() -> provider.generateMappingSuggestion(sampleRequest()))
                .isInstanceOf(AiProviderException.class)
                .hasMessage("provider import mapping suggestion payload has invalid headerPosition");
    }

    private ImportJobMappingSuggestionProviderRequest sampleRequest() {
        return new ImportJobMappingSuggestionProviderRequest(
                "import-ai-mapping-suggestion-provider-test-1",
                302L,
                "gpt-4.1-mini",
                1000,
                new ImportJobMappingSuggestionPrompt("import-mapping-suggestion-v1", "system", "user")
        );
    }
}
