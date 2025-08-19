package com.chriscasey.codechallenger.challenge;

import com.chriscasey.codechallenger.challenge.dto.GeneratedChallenge;
import com.chriscasey.codechallenger.llm.OpenAiResponsesClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class CodeChallengeGeneratorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private OpenAiResponsesClient openAi;
    private CodeChallengeGenerator generator;

    @BeforeEach
    void setUp() {
        openAi = mock(OpenAiResponsesClient.class);
        generator = new CodeChallengeGenerator(openAi, objectMapper);
    }

    @Test
    void generate_parses_output_text_plain_json() {
        // output_text contains plain JSON object as a string
        String payload = "{\"title\":\"T\",\"description\":\"D\",\"solution\":7,\"difficulty\":3}";
        when(openAi.createResponse(anyString(), anyString()))
                .thenReturn(nodeWithOutputText(payload));

        GeneratedChallenge gc = generator.generate(3);

        assertThat(gc.title()).isEqualTo("T");
        assertThat(gc.description()).isEqualTo("D");
        assertThat(gc.solution()).isEqualTo(7);
        assertThat(gc.difficulty()).isEqualTo(3);
    }

    @Test
    void generate_strips_markdown_code_fences() {
        String fenced = """
                ```json
                { "title":"A", "description":"B", "solution":42, "difficulty":2 }
                ```
                """;
        when(openAi.createResponse(anyString(), anyString()))
                .thenReturn(nodeWithOutputText(fenced));

        GeneratedChallenge gc = generator.generate(2);

        assertThat(gc.title()).isEqualTo("A");
        assertThat(gc.solution()).isEqualTo(42);
    }

    // Java
    // Place these inside CodeChallengeGeneratorTest

    private JsonNode nodeWithOutputText(String textContent) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("output_text", textContent);
        return root;
    }

    private JsonNode responsesShape(String textContent) {
        ObjectNode root = objectMapper.createObjectNode();
        ArrayNode output = root.putArray("output");
        ObjectNode item = output.addObject();
        ArrayNode content = item.putArray("content");
        ObjectNode c0 = content.addObject();
        c0.put("text", textContent);
        return root;
    }
}
