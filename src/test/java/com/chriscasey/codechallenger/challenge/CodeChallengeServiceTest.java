package com.chriscasey.codechallenger.challenge;

import com.chriscasey.codechallenger.auth.User;
import com.chriscasey.codechallenger.challenge.dto.CodeChallengeResponse;
import com.chriscasey.codechallenger.challenge.dto.GeneratedChallenge;
import com.chriscasey.codechallenger.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CodeChallengeServiceTest {

    private CodeChallengeRepository repository;
    private CodeChallengeGenerator generator;
    private CodeChallengeService service;

    private User user;

    @BeforeEach
    void setUp() {
        repository = mock(CodeChallengeRepository.class);
        generator = mock(CodeChallengeGenerator.class);
        service = new CodeChallengeService(repository, generator);

        user = new User();
        // assume User has setters; if not, adjust builder/constructor
        // user.setId(1L); user.setEmail("u@test.com");
    }

    @Test
    void submitAnswer_correct_marksCompleted() {
        CodeChallenge ch = CodeChallenge.builder()
                .id(10L)
                .user(user)
                .title("t")
                .description("d")
                .solution(42)
                .difficulty(1)
                .failedAttempts(0)
                .status(ChallengeStatus.PENDING)
                .build();

        when(repository.findByIdAndUser(10L, user)).thenReturn(Optional.of(ch));

        CodeChallengeResponse resp = service.submitAnswer(user, 10L, 42);

        assertThat(resp.status()).isEqualTo(ChallengeStatus.COMPLETED);
        assertThat(resp.completedAt()).isNotNull();
        assertThat(ch.getStatus()).isEqualTo(ChallengeStatus.COMPLETED);
        assertThat(ch.getCompletedAt()).isNotNull();
        verify(repository, never()).save(any()); // dirty checking
    }

    @Test
    void submitAnswer_wrong_incrementsFailedAttempts() {
        CodeChallenge ch = CodeChallenge.builder()
                .id(11L)
                .user(user)
                .title("t")
                .description("d")
                .solution(42)
                .difficulty(1)
                .failedAttempts(1)
                .status(ChallengeStatus.PENDING)
                .build();

        when(repository.findByIdAndUser(11L, user)).thenReturn(Optional.of(ch));

        CodeChallengeResponse resp = service.submitAnswer(user, 11L, 41);

        assertThat(resp.status()).isEqualTo(ChallengeStatus.PENDING);
        assertThat(resp.failedAttempts()).isEqualTo(2);
        assertThat(ch.getFailedAttempts()).isEqualTo(2);
        verify(repository, never()).save(any());
    }

    @Test
    void submitAnswer_notFound_throws() {
        when(repository.findByIdAndUser(12L, user)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.submitAnswer(user, 12L, 1))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void skipChallenge_marksSkipped_and_generatesNew() {
        CodeChallenge ch = CodeChallenge.builder()
                .id(13L)
                .user(user)
                .title("t")
                .description("d")
                .solution(7)
                .difficulty(1)
                .failedAttempts(0)
                .status(ChallengeStatus.PENDING)
                .build();

        when(repository.findByIdAndUser(13L, user)).thenReturn(Optional.of(ch));
        when(repository.countByUserAndStatus(eq(user), eq(ChallengeStatus.COMPLETED))).thenReturn(0L);
        when(generator.generate(anyInt()))
                .thenReturn(new GeneratedChallenge("nt", "nd", 9, 1));

        ArgumentCaptor<CodeChallenge> saveCaptor = ArgumentCaptor.forClass(CodeChallenge.class);
        when(repository.save(saveCaptor.capture())).thenAnswer(inv -> {
            CodeChallenge saved = inv.getArgument(0);
            // simulate id assignment
            return saved.toBuilder().id(99L).build();
        });

        service.skipChallenge(13L, user);

        assertThat(ch.getStatus()).isEqualTo(ChallengeStatus.SKIPPED);
        assertThat(ch.getCompletedAt()).isNotNull();

        CodeChallenge newCh = saveCaptor.getValue();
        assertThat(newCh.getUser()).isEqualTo(user);
        assertThat(newCh.getStatus()).isEqualTo(ChallengeStatus.PENDING);
        assertThat(newCh.getFailedAttempts()).isZero();
    }
}
