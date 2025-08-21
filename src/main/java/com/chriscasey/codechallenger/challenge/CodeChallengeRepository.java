package com.chriscasey.codechallenger.challenge;

import com.chriscasey.codechallenger.auth.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CodeChallengeRepository extends JpaRepository<CodeChallenge, Long> {
    long countByUserAndStatus(User user, ChallengeStatus status);

    Optional<CodeChallenge> findByIdAndUser(Long challengeId, User user);

    List<CodeChallenge> findByUser(User user);

    // Fetch challenges for multiple users to avoid N+1 in admin listing
    List<CodeChallenge> findByUserIn(List<User> users);

    // Or if you want to be more explicit, add this method:
    default long countIncompleteByUser(User user) {
        return countByUserAndStatus(user, ChallengeStatus.PENDING);
    }

}
