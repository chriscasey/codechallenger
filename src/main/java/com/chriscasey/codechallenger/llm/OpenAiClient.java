package com.chriscasey.codechallenger.llm;

import com.chriscasey.codechallenger.challenge.dto.GeneratedChallenge;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class OpenAiClient {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.model}")
    private String model;

    private final OkHttpClient httpClient = new OkHttpClient();

    public GeneratedChallenge generateChallenge(int difficulty) {
        String prompt = buildPrompt(difficulty);

        String requestBody = """
        {
          "model": "%s",
          "messages": [
            {
              "role": "user",
              "content": "%s"
            }
          ],
          "temperature": 0.7
        }
        """.formatted(model, prompt.replace("\"", "\\\"").replace("\n", "\\n"));

        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("OpenAI API call failed: " + response);
            }

            String body = response.body().string();
            JsonNode json = objectMapper.readTree(body);
            String content = json.get("choices").get(0).get("message").get("content").asText();

            JsonNode parsed = objectMapper.readTree(content);
            return new GeneratedChallenge(
                    parsed.get("title").asText(),
                    parsed.get("description").asText(),
                    parsed.get("solution").asInt(),
                    parsed.has("difficulty") ? parsed.get("difficulty").asInt() : difficulty,
                    prompt
            );

        } catch (IOException e) {
            throw new RuntimeException("Failed to generate challenge", e);
        }
    }

    private String buildPrompt(int difficulty) {
        return """
        Generate a programming challenge of difficulty %d.
        The challenge should be solvable in any language.
        The answer should be a single integer.
        
        Respond in this exact JSON format:
        {
          "title": "...",
          "description": "...",
          "solution": 123
        }
        """.formatted(difficulty);
    }
}
