package com.chriscasey.codechallenger.challenge;

import com.chriscasey.codechallenger.auth.User;
import com.chriscasey.codechallenger.challenge.dto.CodeChallengeResponse;
import com.chriscasey.codechallenger.challenge.dto.GeneratedChallenge;
import com.chriscasey.codechallenger.challenge.mapper.CodeChallengeMapper;
import com.chriscasey.codechallenger.exception.NotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CodeChallengeService {

    private final CodeChallengeRepository repository;
    private final CodeChallengeGenerator generator;

    public List<CodeChallenge> getAllForUser(User user) {
        return repository.findByUser(user);
    }

    public void createChallenge(User user) {
        int difficulty = determineDifficulty(user);
        GeneratedChallenge generated = generator.generate(difficulty);

        CodeChallenge challenge = CodeChallenge.builder()
                .user(user)
                .title(generated.title())
                .description(generated.description())
                .solution(generated.solution())
                .difficulty(difficulty)
                .status(ChallengeStatus.PENDING)
                .build();

        repository.save(challenge);
    }

    @Transactional
    public CodeChallengeResponse submitAnswer(User user, Long challengeId, int answer) {
        CodeChallenge challenge = (CodeChallenge) repository.findByIdAndUser(challengeId, user)
                .orElseThrow(() -> new NotFoundException("Challenge not found"));

        if (challenge.getStatus() != ChallengeStatus.PENDING) {
            throw new IllegalStateException("Challenge already completed or skipped");
        }

        if (challenge.getSolution() == answer) {
            challenge.setStatus(ChallengeStatus.COMPLETED);
            challenge.setCompletedAt(LocalDateTime.now());
        } else {
            challenge.setFailedAttempts(challenge.getFailedAttempts() + 1);
        }


        repository.save(challenge);
        return CodeChallengeMapper.toResponse(challenge);
    }

    @Transactional
    public void skipChallenge(Long challengeId, User user) {
        CodeChallenge challenge = (CodeChallenge) repository.findByIdAndUser(challengeId, user)
                .orElseThrow(() -> new NotFoundException("Challenge not found or not owned by user"));

        if (challenge.getStatus() != ChallengeStatus.PENDING) {
            throw new IllegalStateException("Only pending challenges can be skipped");
        }

        challenge.setStatus(ChallengeStatus.SKIPPED);
        challenge.setCompletedAt(LocalDateTime.now());
        repository.save(challenge);

        createChallenge(user); // Create a new challenge with same user + updated difficulty
    }


    private int determineDifficulty(User user) {
        // Basic implementation: make it harder as user completes more
        long completed = repository.countByUserAndStatus(user, ChallengeStatus.COMPLETED);
        return (int) Math.min(5, 1 + completed / 3); // increase difficulty every 3 correct
    }
}
