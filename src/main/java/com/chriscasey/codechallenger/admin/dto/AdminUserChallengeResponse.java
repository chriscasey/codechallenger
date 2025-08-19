package com.chriscasey.codechallenger.admin.dto;

import java.util.List;

public record AdminUserChallengeResponse(
        Long userId,
        String email,
        List<AdminChallengeResponse> challenges
) {}
