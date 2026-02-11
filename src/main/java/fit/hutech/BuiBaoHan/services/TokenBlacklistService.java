package fit.hutech.BuiBaoHan.services;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fit.hutech.BuiBaoHan.entities.TokenBlacklist;
import fit.hutech.BuiBaoHan.entities.TokenBlacklist.BlacklistReason;
import fit.hutech.BuiBaoHan.entities.TokenBlacklist.TokenType;
import fit.hutech.BuiBaoHan.repositories.ITokenBlacklistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service quản lý Token Blacklist.
 * 
 * Mục đích: Invalidate JWT tokens trước khi hết hạn tự nhiên
 * 
 * Các trường hợp blacklist:
 * - User logout
 * - Password change
 * - Admin revoke
 * - Security breach detected
 * - Account locked
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TokenBlacklistService {

    private final ITokenBlacklistRepository tokenBlacklistRepository;

    /**
     * Thêm token vào blacklist
     */
    public TokenBlacklist blacklistToken(String token, Instant expiryDate, BlacklistReason reason, 
                                         Long userId, TokenType tokenType) {
        // Kiểm tra đã tồn tại chưa
        if (tokenBlacklistRepository.existsByToken(token)) {
            log.debug("Token already blacklisted");
            return tokenBlacklistRepository.findByToken(token).orElse(null);
        }

        TokenBlacklist blacklistedToken = TokenBlacklist.builder()
                .token(token)
                .expiryDate(expiryDate)
                .blacklistedAt(Instant.now())
                .reason(reason)
                .userId(userId)
                .tokenType(tokenType)
                .build();

        TokenBlacklist saved = tokenBlacklistRepository.save(blacklistedToken);
        log.info("Token blacklisted. Reason: {}, UserId: {}", reason, userId);
        
        return saved;
    }

    /**
     * Blacklist access token
     */
    public TokenBlacklist blacklistAccessToken(String token, Instant expiryDate, 
                                                BlacklistReason reason, Long userId) {
        return blacklistToken(token, expiryDate, reason, userId, TokenType.ACCESS);
    }

    /**
     * Blacklist refresh token
     */
    public TokenBlacklist blacklistRefreshToken(String token, Instant expiryDate, 
                                                 BlacklistReason reason, Long userId) {
        return blacklistToken(token, expiryDate, reason, userId, TokenType.REFRESH);
    }

    /**
     * Kiểm tra token có trong blacklist không
     */
    @Transactional(readOnly = true)
    public boolean isBlacklisted(String token) {
        return tokenBlacklistRepository.isTokenBlacklisted(token, Instant.now());
    }

    /**
     * Cleanup expired tokens (scheduled job)
     * Token đã hết hạn tự nhiên không cần giữ trong blacklist nữa
     */
    public int cleanupExpiredTokens() {
        int deleted = tokenBlacklistRepository.deleteExpiredTokens(Instant.now());
        log.info("Cleaned up {} expired blacklist entries", deleted);
        return deleted;
    }

    /**
     * Đếm số tokens bị blacklist của user
     */
    @Transactional(readOnly = true)
    public long countBlacklistedByUser(Long userId) {
        return tokenBlacklistRepository.countByUserId(userId);
    }

    /**
     * Xóa blacklist entries của user (khi xóa user)
     */
    public int deleteByUserId(Long userId) {
        return tokenBlacklistRepository.deleteByUserId(userId);
    }
}
