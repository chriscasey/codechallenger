package com.chriscasey.codechallenger.challenge;

import com.chriscasey.codechallenger.auth.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "code_challenge",
        indexes = {
                @Index(name = "idx_challenge_user", columnList = "user_id"),
                @Index(name = "idx_challenge_user_status", columnList = "user_id,status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class CodeChallenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Optimistic lock version column
    @Version
    private Long version;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Column(nullable = false)
    private int solution;

    @Column(nullable = false)
    private int difficulty;

    @Column(nullable = false)
    private int failedAttempts;

    @Column(name = "last_attempt_time")
    private LocalDateTime lastAttemptTime;

    private LocalDateTime completedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ChallengeStatus status;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
