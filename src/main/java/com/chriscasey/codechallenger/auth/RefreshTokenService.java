package com.chriscasey.codechallenger.auth;

import com.chriscasey.codechallenger.exception.RefreshTokenException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-expiration}")
    private Long refreshTokenExpirationMs;

    public RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(refreshTokenExpirationMs))
                .build();

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

    @Transactional
    public RefreshToken rotateRefreshToken(User user, String oldToken) {
        refreshTokenRepository.deleteByToken(oldToken);
        return createRefreshToken(user);
    }
}
