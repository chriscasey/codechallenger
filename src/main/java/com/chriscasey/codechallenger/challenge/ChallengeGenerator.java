package com.chriscasey.codechallenger.challenge;

import org.springframework.stereotype.Component;

@Component
public class ChallengeGenerator {

    public Challenge generate(int difficulty) {
        // 🧪 Placeholder implementation — you'll replace this with an LLM API call
        return new Challenge(
                "Sum from 1 to N",
                "Write a program that calculates the sum of all integers from 1 to 100.",
                "5050",
                difficulty
        );
    }
}
