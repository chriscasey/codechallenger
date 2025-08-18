package com.chriscasey.codechallenger.llm;

import com.chriscasey.codechallenger.challenge.dto.GeneratedChallenge;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;

@Slf4j
@Service
public class OpenAiClient {

    private final OkHttpClient http;
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.url:https://api.openai.com/v1/chat/completions}")
    private String apiUrl;

    @Value("${openai.model:gpt-4o-mini}")
    private String model;

    @Value("${openai.temperature:0.7}")
    private double temperature;

    public OpenAiClient(@Value("${openai.timeout.seconds:30}") int timeoutSeconds) {
        this.http = new OkHttpClient.Builder()
                .callTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();
    }

    public GeneratedChallenge generateChallenge(int difficulty) {
        final String system = """
            You generate language-agnostic programming challenges.
            The result MUST be valid JSON only (no prose, no code fences).
            The answer/solution MUST be a single integer.
            """;

        final String user = """
            Generate one programming challenge of difficulty %d.
            Constraints:
            - Language-agnostic prompt.
            - Solution must be an integer.
            Respond with EXACT JSON:
            {
              "title": "...",
              "description": "...",
              "solution": 123
            }
            """.formatted(difficulty);

        String bodyJson = """
            {
              "model": "%s",
              "messages": [
                {"role":"system","content": %s},
                {"role":"user","content": %s}
              ],
              "temperature": %s
            }
            """.formatted(
                model,
                jsonString(system),
                jsonString(user),
                Double.toString(temperature)
        );

        Request req = new Request.Builder()
                .url(apiUrl)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(bodyJson, MediaType.parse("application/json")))
                .build();

        // basic retry on 429
        for (int attempt = 1; attempt <= 2; attempt++) {
            try (Response res = http.newCall(req).execute()) {
                if (res.code() == 429 && attempt == 1) {
                    log.warn("OpenAI rate-limited (429). Retrying once...");
                    try { Thread.sleep(1200); } catch (InterruptedException ignored) {}
                    continue;
                }
                if (!res.isSuccessful()) {
                    throw new RuntimeException("OpenAI call failed: HTTP " + res.code() + " - " + safeBody(res));
                }
                String content = extractContent(safeBody(res));
                String cleaned = stripCodeFences(content).trim();
                JsonNode json = mapper.readTree(cleaned);

                String title = json.path("title").asText(null);
                String description = json.path("description").asText(null);
                if (title == null || description == null || !json.has("solution")) {
                    throw new IllegalStateException("Malformed JSON from OpenAI: " + cleaned);
                }
                int solution = json.get("solution").asInt();

                return new GeneratedChallenge(
                        title,
                        description,
                        solution,
                        difficulty,
                        user
                );
            } catch (IOException e) {
                throw new RuntimeException("OpenAI request failed", e);
            }
        }
        // unreachable
        throw new IllegalStateException("Unexpected retry logic fallthrough");
    }

    private static String stripCodeFences(String s) {
        // removes ```json ... ``` or ``` ... ```
        return s.replaceAll("(?is)^\\s*```(?:json)?\\s*|\\s*```\\s*$", "");
    }

    private static String extractContent(String responseBody) throws IOException {
        // responseBody is the full chat completion result; pull choices[0].message.content
        ObjectMapper m = new ObjectMapper();
        JsonNode root = m.readTree(responseBody);
        JsonNode choices = root.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            throw new IllegalStateException("OpenAI response missing choices: " + responseBody);
        }
        JsonNode content = choices.get(0).path("message").path("content");
        if (content.isMissingNode()) {
            throw new IllegalStateException("OpenAI response missing message.content: " + responseBody);
        }
        return content.asText();
    }

    private static String jsonString(String s) {
        // minimal JSON string escaper
        return "\"" + s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r") + "\"";
    }

    private static String safeBody(Response res) {
        try {
            return res.body() != null ? res.body().string() : "<empty>";
        } catch (IOException e) {
            return "<unreadable>";
        }
    }
}
