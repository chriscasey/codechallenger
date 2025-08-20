package com.chriscasey.codechallenger.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import okhttp3.MediaType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import com.theokanning.openai.service.OpenAiService;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OpenAiResponsesClient {

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final ObjectMapper objectMapper;

    @Value("${openai.api.base:}")
    private String baseUrl;

    @Value("${openai.api.key}")
    private String apiKey;

    // e.g. gpt-4o-mini (ensure the model is available for your key)
    @Value("${openai.model:gpt-4o-mini}")
    private String model;

    // Optional tuning (leave blank to skip)
    @Value("${openai.verbosity:}")
    private String verbosity;            // low | medium | high

    @Value("${openai.reasoning.effort:}")
    private String reasoningEffort;      // minimal | low | medium | high

    @Value("${openai.timeout.seconds:30}")
    private long timeoutSeconds;

    @Value("${openai.temperature:1}")
    private double temperature;

    /**
     * Calls OpenAI Chat Completions and wraps the assistant text into:
     * { "output_text": "..." } so downstream parsing remains unchanged.
     */
    public JsonNode createResponse(String system, String user) {
        OpenAiService service = new OpenAiService(apiKey, Duration.ofSeconds(timeoutSeconds));

        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model(model)
                .messages(List.of(
                        new ChatMessage("system", system),
                        new ChatMessage("user", user)
                ))
                .temperature(temperature)
                .n(1)
                .build();

        String content;
        try {
            var result = service.createChatCompletion(request);
            if (result == null || result.getChoices() == null || result.getChoices().isEmpty()) {
                throw new RuntimeException("OpenAI returned no choices");
            }
            var msg = result.getChoices().get(0).getMessage();
            content = (msg != null && msg.getContent() != null) ? msg.getContent() : "";
        } catch (Exception e) {
            throw new RuntimeException("OpenAI chat call failed", e);
        }

        var node = objectMapper.createObjectNode();
        node.put("output_text", content);
        return node;
    }
}
