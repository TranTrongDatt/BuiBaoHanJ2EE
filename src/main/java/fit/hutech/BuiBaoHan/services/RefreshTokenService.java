package fit.hutech.BuiBaoHan.services;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fit.hutech.BuiBaoHan.entities.RefreshToken;
import fit.hutech.BuiBaoHan.entities.User;
import fit.hutech.BuiBaoHan.repositories.IRefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service quản lý Refresh Token với Token Rotation Strategy.
 * 
 * Features:
 * - Tạo refresh token mới
 * - Validate và refresh token
 * - Token rotation (tạo token mới, revoke token cũ)
 * - Detect token reuse attack
 * - Revoke tất cả tokens của user
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RefreshTokenService {

    private final IRefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-expiration:604800000}")
    private long refreshTokenDurationMs; // 7 days default

    /**
     * Tạo refresh token mới cho user
     */
    public RefreshToken createRefreshToken(User user, String deviceInfo, String ipAddress) {
        // Tạo token family mới cho mỗi login session
        String tokenFamily = UUID.randomUUID().toString();
        
        return createRefreshTokenInFamily(user, deviceInfo, ipAddress, tokenFamily);
    }

    /**
     * Tạo refresh token mới trong cùng một token family (rotation)
     */
    public RefreshToken createRefreshTokenInFamily(User user, String deviceInfo, String ipAddress, String tokenFamily) {
        Instant now = Instant.now();
        Instant expiryDate = now.plus(refreshTokenDurationMs, ChronoUnit.MILLIS);

        RefreshToken refreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiryDate(expiryDate)
                .createdAt(now)
                .revoked(false)
                .deviceInfo(deviceInfo)
                .ipAddress(ipAddress)
                .tokenFamily(tokenFamily)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    /**
     * Tìm refresh token theo token string
     */
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    /**
     * Validate refresh token
     * Trả về token nếu valid, empty nếu invalid
     */
    public Optional<RefreshToken> validateRefreshToken(String token) {
        return refreshTokenRepository.findValidToken(token, Instant.now());
    }

    /**
     * Rotate refresh token: revoke token cũ và tạo token mới
     * Giữ nguyên token family để detect reuse attack
     */
    public RefreshToken rotateRefreshToken(RefreshToken oldToken, String deviceInfo, String ipAddress) {
        // Revoke token cũ
        oldToken.setRevoked(true);
        oldToken.setRevokeReason("TOKEN_ROTATION");
        refreshTokenRepository.save(oldToken);

        // Tạo token mới trong cùng family
        return createRefreshTokenInFamily(
                oldToken.getUser(),
                deviceInfo,
                ipAddress,
                oldToken.getTokenFamily()
        );
    }

    /**
     * Xử lý token reuse attack
     * Khi phát hiện token đã bị revoke được sử dụng lại
     * -> Revoke tất cả tokens trong family (attacker và victim đều mất access)
     */
    public void handleTokenReuseAttack(RefreshToken revokedToken) {
        log.warn("Token reuse detected for family: {}. Revoking all tokens in family.", 
                revokedToken.getTokenFamily());
        
        refreshTokenRepository.revokeAllByTokenFamily(
                revokedToken.getTokenFamily(),
                "SECURITY_BREACH_TOKEN_REUSE"
        );
    }

    /**
     * Revoke tất cả refresh tokens của user
     * Dùng khi: logout all devices, password change, security breach
     */
    public int revokeAllUserTokens(Long userId, String reason) {
        return refreshTokenRepository.revokeAllByUserId(userId, reason);
    }

    /**
     * Revoke một refresh token cụ thể
     */
    public void revokeToken(RefreshToken token, String reason) {
        token.setRevoked(true);
        token.setRevokeReason(reason);
        refreshTokenRepository.save(token);
    }

    /**
     * Cleanup expired tokens (scheduled job)
     */
    public int cleanupExpiredTokens() {
        int deleted = refreshTokenRepository.deleteExpiredTokens(Instant.now());
        log.info("Cleaned up {} expired refresh tokens", deleted);
        return deleted;
    }

    /**
     * Đếm số active sessions của user
     */
    public long countActiveSessions(Long userId) {
        return refreshTokenRepository.countActiveTokensByUserId(userId, Instant.now());
    }
}
