package com.chriscasey.codechallenger.challenge.dto;

public record CodeChallengeRequest(
        String title,
        String challengePrompt,
        int difficultyRating
) {}
