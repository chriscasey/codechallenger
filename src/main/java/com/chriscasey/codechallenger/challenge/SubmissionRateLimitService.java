package com.chriscasey.codechallenger.challenge;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SubmissionRateLimitService {
    
    private final CodeChallengeRepository codeChallengeRepository;
    
    @Value("${app.submission-cooldown-minutes:5}")
    private int cooldownMinutes;
    
    public boolean canSubmit(CodeChallenge codeChallenge) {
        if (codeChallenge.getLastAttemptTime() == null) {
            return true;
        }
        
        LocalDateTime nextAllowedTime = codeChallenge.getLastAttemptTime()
            .plusMinutes(cooldownMinutes);
        
        return LocalDateTime.now().isAfter(nextAllowedTime);
    }
    
    public long getRemainingCooldownMinutes(CodeChallenge codeChallenge) {
        if (codeChallenge.getLastAttemptTime() == null) {
            return 0;
        }
        
        LocalDateTime nextAllowedTime = codeChallenge.getLastAttemptTime()
            .plusMinutes(cooldownMinutes);
        
        if (LocalDateTime.now().isAfter(nextAllowedTime)) {
            return 0;
        }
        
        return java.time.Duration.between(LocalDateTime.now(), nextAllowedTime).toMinutes();
    }
    
    public void recordAttempt(CodeChallenge codeChallenge) {
        codeChallenge.setLastAttemptTime(LocalDateTime.now());
        codeChallengeRepository.save(codeChallenge);
    }
}
