package com.chriscasey.codechallenger.challenge;

import com.chriscasey.codechallenger.challenge.dto.GeneratedChallenge;
import com.chriscasey.codechallenger.llm.OpenAiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class CodeChallengeGenerator {

    private final OpenAiClient openAiClient;

    public GeneratedChallenge generate(int difficulty) {
        return openAiClient.generateChallenge(difficulty);
    }
}
