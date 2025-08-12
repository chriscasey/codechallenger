package com.chriscasey.codechallenger.challenge;

import com.chriscasey.codechallenger.auth.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CodeChallengeRepository extends JpaRepository<CodeChallenge, Long> {
    long countByUserAndStatus(User user, ChallengeStatus status);

    Optional<Object> findByIdAndUser(Long challengeId, User user);

    List<CodeChallenge> findByUser(User user);

    List<CodeChallenge> findAllByUser(User user);
}
