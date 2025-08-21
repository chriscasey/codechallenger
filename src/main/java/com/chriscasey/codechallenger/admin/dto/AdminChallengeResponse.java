package com.chriscasey.codechallenger.admin.dto;

import com.chriscasey.codechallenger.challenge.ChallengeStatus;

import java.time.LocalDateTime;

public record AdminChallengeResponse(
        Long id,
        String title,
        String description,
        int difficulty,
        ChallengeStatus status,
        int failedAttempts,
        LocalDateTime completedAt,
        int solution
) {}
