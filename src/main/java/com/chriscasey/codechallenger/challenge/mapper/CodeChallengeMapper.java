package com.chriscasey.codechallenger.challenge.mapper;

import com.chriscasey.codechallenger.admin.dto.AdminChallengeResponse;
import com.chriscasey.codechallenger.challenge.CodeChallenge;
import com.chriscasey.codechallenger.challenge.dto.CodeChallengeResponse;

public class CodeChallengeMapper {

    public static CodeChallengeResponse toResponse(CodeChallenge challenge) {
        return new CodeChallengeResponse(
                challenge.getId(),
                challenge.getTitle(),
                challenge.getDescription(),
                challenge.getDifficulty(),
                challenge.getStatus(),
                challenge.getFailedAttempts(),
                challenge.getCompletedAt()
        );
    }

    // Admin-only mapping: includes solution
    public static AdminChallengeResponse toAdminResponse(CodeChallenge challenge) {
        return new AdminChallengeResponse(
                challenge.getId(),
                challenge.getTitle(),
                challenge.getDescription(),
                challenge.getDifficulty(),
                challenge.getStatus(),
                challenge.getFailedAttempts(),
                challenge.getCompletedAt(),
                challenge.getSolution()
        );
    }
}
