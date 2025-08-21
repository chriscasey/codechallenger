package com.chriscasey.codechallenger.challenge;

import com.chriscasey.codechallenger.auth.User;
import com.chriscasey.codechallenger.challenge.dto.CodeChallengeResponse;
import com.chriscasey.codechallenger.challenge.dto.GeneratedChallenge;
import com.chriscasey.codechallenger.challenge.mapper.CodeChallengeMapper;
import com.chriscasey.codechallenger.exception.NotFoundException;
import com.chriscasey.codechallenger.exception.TooManyChallengesException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CodeChallengeService {

    private final CodeChallengeRepository repository;
    private final CodeChallengeGenerator generator;

    private static final int MAX_INCOMPLETE_CHALLENGES = 5;

    @Transactional(readOnly = true)
    public List<CodeChallenge> getAllForUser(User user) {
        return repository.findByUser(user);
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

        // Rely on JPA dirty checking — no explicit save needed within @Transactional
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

        // No explicit save; JPA will flush changes at transaction commit

        // Create a new challenge with same user + updated difficulty
        int nextDifficulty = determineDifficulty(user);
        generateAndPersistChallenge(user, nextDifficulty);
    }

    @Transactional
    public CodeChallengeResponse generateNewChallenge(User user) {
        validateChallengeLimit(user);
        int difficulty = determineDifficulty(user);
        CodeChallenge created = generateAndPersistChallenge(user, difficulty);
        return CodeChallengeMapper.toResponse(created);
    }

    @Transactional
    public CodeChallengeResponse generateNewChallenge(User user, Integer overrideDifficulty) {
        validateChallengeLimit(user);
        int difficulty = (overrideDifficulty != null) ? clampDifficulty(overrideDifficulty) : determineDifficulty(user);
        CodeChallenge created = generateAndPersistChallenge(user, difficulty);
        return CodeChallengeMapper.toResponse(created);
    }

    // Validate that user doesn't have too many incomplete challenges
    private void validateChallengeLimit(User user) {
        long incompleteCount = repository.countByUserAndStatus(user, ChallengeStatus.PENDING);
        if (incompleteCount >= MAX_INCOMPLETE_CHALLENGES) {
            throw new TooManyChallengesException(
                String.format("You already have %d incomplete challenges. Complete or skip some before generating new ones.", 
                    MAX_INCOMPLETE_CHALLENGES)
            );
        }
    }

    // Consolidated generation/persist logic
    private CodeChallenge generateAndPersistChallenge(User user, int difficulty) {
        GeneratedChallenge generated = generator.generate(difficulty);
        CodeChallenge challenge = CodeChallenge.builder()
                .user(user)
                .title(generated.title())
                .description(generated.description())
                .solution(generated.solution())
                .difficulty(difficulty)
                .status(ChallengeStatus.PENDING)
                .failedAttempts(0)
                .completedAt(null)
                .build();
        return repository.save(challenge);
    }

    private int clampDifficulty(int difficulty) {
        if (difficulty < 1) return 1;
        if (difficulty > 5) return 5;
        return difficulty;
    }

    private int determineDifficulty(User user) {
        // Basic implementation: make it harder as user completes more
        long completed = repository.countByUserAndStatus(user, ChallengeStatus.COMPLETED);
        return (int) Math.min(5, 1 + completed / 3); // increase difficulty every 3 correct
    }
}