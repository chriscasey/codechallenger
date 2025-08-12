package com.chriscasey.codechallenger.admin.dto;

import com.chriscasey.codechallenger.challenge.dto.CodeChallengeResponse;

import java.util.List;

public record UserChallengeResponse(
        Long userId,
        String email,
        List<CodeChallengeResponse> challenges
) {}
