package com.chriscasey.codechallenger.challenge.mapper;

import com.chriscasey.codechallenger.challenge.CodeChallenge;
import com.chriscasey.codechallenger.challenge.dto.CodeChallengeResponse;

public class CodeChallengeMapper {

    public static CodeChallengeResponse toResponse(CodeChallenge challenge) {
        return new CodeChallengeResponse(
                challenge.getId(),
                challenge.getTitle(),
                challenge.getPrompt(),
                challenge.getDifficulty(),
                challenge.getStatus(),
                challenge.getFailedAttempts(),
                challenge.getCompletedAt()
        );
    }
}
