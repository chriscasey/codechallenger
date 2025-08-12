package com.chriscasey.codechallenger.challenge;

import com.chriscasey.codechallenger.auth.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class CodeChallenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String prompt;

    private String description;

    private int solution;

    private int difficulty;

    private int failedAttempts;

    private LocalDateTime completedAt;

    @Enumerated(EnumType.STRING)
    private ChallengeStatus status;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}
