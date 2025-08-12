package com.chriscasey.codechallenger.challenge.dto;

import com.chriscasey.codechallenger.challenge.ChallengeStatus;

import java.time.LocalDateTime;

public record CodeChallengeResponse(
        Long id,
        String title,
        String prompt,
        int difficulty,
        ChallengeStatus status,
        int failedAttempts,
        LocalDateTime completedAt
) {}
