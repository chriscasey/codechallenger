package com.chriscasey.codechallenger.challenge;

import com.chriscasey.codechallenger.challenge.dto.GeneratedChallenge;
import com.chriscasey.codechallenger.llm.OpenAiResponsesClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class CodeChallengeGenerator {

    private static final Pattern CODE_FENCE_PATTERN = Pattern.compile("(?s)```(?:json)?\\s*(\\{.*?\\})\\s*```");

    private final OpenAiResponsesClient openAi;
    private final ObjectMapper objectMapper;

    public GeneratedChallenge generate(int difficulty) {
        int safeDifficulty = clampDifficulty(difficulty);

        String system = """
            You generate language-agnostic programming puzzles.

            Return ONLY a single JSON object with EXACTLY these fields:
            - title (string)
            - description (string)
            - solution (integer)
            - difficulty (integer)

            No code fences, no extra keys, no surrounding text. Output JSON object ONLY.
            """;

        String user = """
            Create a puzzle at difficulty %d.
            Theme: arrays, loops, and simple math.
            Ensure the answer is a single integer.
            """.formatted(safeDifficulty);

        JsonNode resp = openAi.createResponse(system, user);
        String content = extractText(resp); // get the model's raw JSON string
        String cleaned = cleanContent(content); // strip fences / surrounding prose if present

        JsonNode parsed;
        try {
            parsed = objectMapper.readTree(cleaned);
        } catch (Exception ex) {
            throw new com.chriscasey.codechallenger.exception.LlmParseException(
                    "Failed to parse LLM JSON", cleaned, ex
            );
        }

        String title = reqText(parsed, "title", cleaned);
        String description = reqText(parsed, "description", cleaned);
        int solution = reqInt(parsed, "solution", cleaned);
        int outDifficulty = parsed.has("difficulty") ? reqInt(parsed, "difficulty", cleaned) : safeDifficulty;

        return new GeneratedChallenge(title, description, solution, outDifficulty);
    }

    /**
     * Try common shapes of the Responses API to pull the text content.
     * If your SDK already exposes "output_text", prefer that.
     */
    private String extractText(JsonNode resp) {
        // Preferred helper some SDKs expose
        if (resp.has("output_text")) {
            return resp.get("output_text").asText();
        }

        // Generic traversal of responses structure
        if (resp.has("output") && resp.get("output").isArray() && resp.get("output").size() > 0) {
            JsonNode first = resp.get("output").get(0);
            if (first.has("content") && first.get("content").isArray() && first.get("content").size() > 0) {
                JsonNode c0 = first.get("content").get(0);
                if (c0.has("text") && !c0.get("text").isNull()) {
                    return c0.get("text").asText();
                }
            }
        }

        // Last resort: raw JSON (may cause parse error if it's not a pure object)
        return resp.toString();
    }

    // Remove typical markdown code fences and keep the first JSON object if present.
    private String cleanContent(String content) {
        if (content == null) return "";
        Matcher m = CODE_FENCE_PATTERN.matcher(content);
        if (m.find()) {
            return m.group(1);
        }
        // If no fence, try to trim whitespace and any leading/trailing prose
        // by extracting the first {...} block.
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return content.substring(start, end + 1).trim();
        }
        return content.trim();
    }

    private int clampDifficulty(int difficulty) {
        if (difficulty < 1) return 1;
        if (difficulty > 5) return 5;
        return difficulty;
    }

    // --- Defensive field accessors to avoid NPEs ---

    private static String reqText(JsonNode obj, String field, String raw) {
        JsonNode v = obj.get(field);
        if (v == null || v.isNull() || !v.isTextual()) {
            throw new com.chriscasey.codechallenger.exception.LlmParseException(
                    "LLM JSON missing string field '" + field + "'", raw, null
            );
        }
        return v.asText();
    }

    private static int reqInt(JsonNode obj, String field, String raw) {
        JsonNode v = obj.get(field);
        if (v == null || v.isNull() || !v.canConvertToInt()) {
            throw new com.chriscasey.codechallenger.exception.LlmParseException(
                    "LLM JSON missing integer field '" + field + "'", raw, null
            );
        }
        return v.asInt();
    }
}