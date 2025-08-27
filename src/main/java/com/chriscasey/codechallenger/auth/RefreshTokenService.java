package com.chriscasey.codechallenger.auth;

import com.chriscasey.codechallenger.exception.RefreshTokenException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${security.jwt.refresh-token.expiration}")
    private Long refreshTokenExpirationMs;

    public RefreshToken createRefreshToken(User user) {
        // Check if a refresh token already exists for this user
        Optional<RefreshToken> existingTokenOpt = refreshTokenRepository.findByUser(user);
    
        RefreshToken refreshToken;
        if (existingTokenOpt.isPresent()) {
            refreshToken = existingTokenOpt.get();
            refreshToken.setToken(UUID.randomUUID().toString());
            refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenExpirationMs));
        } else {
            // Create new token
            refreshToken = new RefreshToken();
            refreshToken.setUser(user);
            refreshToken.setToken(UUID.randomUUID().toString());
            refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenExpirationMs));
        }
    
        return refreshTokenRepository.save(refreshToken);
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(token);
            throw new RefreshTokenException("Refresh token has expired. Please log in again.");
        }
        return token;
    }

    public RefreshToken getValidRefreshTokenOrThrow(String token) {
        return refreshTokenRepository.findByToken(token)
                .map(this::verifyExpiration)
                .orElseThrow(() -> new RefreshTokenException("Invalid or expired refresh token"));
    }

}
