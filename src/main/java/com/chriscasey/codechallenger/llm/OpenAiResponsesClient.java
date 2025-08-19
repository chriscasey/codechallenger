package com.chriscasey.codechallenger.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OpenAiResponsesClient {

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final ObjectMapper objectMapper;

    @Value("${openai.api.base}")
    private String baseUrl;

    @Value("${openai.api.key}")
    private String apiKey;

    // e.g. gpt-5.0, gpt-5-mini, etc.
    @Value("${openai.model:gpt-5.0}")
    private String model;

    // Optional tuning (leave blank to skip)
    @Value("${openai.verbosity:}")
    private String verbosity;            // low | medium | high

    @Value("${openai.reasoning.effort:}")
    private String reasoningEffort;      // minimal | low | medium | high

    private final OkHttpClient http = new OkHttpClient();

    /**
     * Calls the OpenAI Responses API (/v1/responses) with text output.
     * - Uses "text.format = json" to strongly bias JSON output.
     * - Moves verbosity under "text.verbosity".
     */
    public JsonNode createResponse(String system, String user) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("model", model);

            // Simple single string input. (If you prefer "input: [{role, content}, ...]" we can switch.)
            body.put("input", system + "\n\n" + user);

            // Optional reasoning controls
            if (reasoningEffort != null && !reasoningEffort.isBlank()) {
                Map<String, Object> reasoning = new HashMap<>();
                reasoning.put("effort", reasoningEffort); // minimal|low|medium|high
                body.put("reasoning", reasoning);
            }

            // ✅ All text-related options go under "text"
            Map<String, Object> text = new HashMap<>();
            text.put("format", "json"); // << moved from response_format
            if (verbosity != null && !verbosity.isBlank()) {
                text.put("verbosity", verbosity); // low|medium|high
            }
            body.put("text", text);

            Request request = new Request.Builder()
                    .url(baseUrl + "/v1/responses")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(objectMapper.writeValueAsBytes(body), JSON))
                    .build();

            try (Response resp = http.newCall(request).execute()) {
                if (!resp.isSuccessful()) {
                    String errorBody = resp.body() != null ? resp.body().string() : "";
                    throw new RuntimeException("OpenAI call failed: HTTP " + resp.code() + " - " + errorBody);
                }
                return objectMapper.readTree(resp.body().byteStream());
            }
        } catch (IOException e) {
            throw new RuntimeException("OpenAI call failed", e);
        }
    }
}
