package fit.hutech.BuiBaoHan.config;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import fit.hutech.BuiBaoHan.repositories.IPasswordResetTokenRepository;
import fit.hutech.BuiBaoHan.repositories.IPersistentLoginRepository;
import fit.hutech.BuiBaoHan.repositories.IRefreshTokenRepository;
import fit.hutech.BuiBaoHan.repositories.ITokenBlacklistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Scheduled job để dọn dẹp các token hết hạn.
 * 
 * Jobs:
 * - Xóa refresh tokens hết hạn (hàng giờ)
 * - Xóa blacklisted tokens đã quá hạn (hàng giờ)
 * - Xóa password reset tokens hết hạn (mỗi 30 phút)
 * - Xóa persistent logins hết hạn (hàng ngày)
 */
@Component
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class TokenCleanupJob {

    private final IRefreshTokenRepository refreshTokenRepository;
    private final ITokenBlacklistRepository tokenBlacklistRepository;
    private final IPasswordResetTokenRepository passwordResetTokenRepository;
    private final IPersistentLoginRepository persistentLoginRepository;
    
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Xóa refresh tokens hết hạn - chạy mỗi giờ.
     * Cron: 0 0 * * * * (mỗi giờ)
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanupExpiredRefreshTokens() {
        try {
            long before = refreshTokenRepository.count();
            int deleted = refreshTokenRepository.deleteExpiredTokens(Instant.now());
            long after = refreshTokenRepository.count();
            
            if (deleted > 0) {
                log.info("[TokenCleanup] Refresh Tokens: Deleted {} expired tokens. Total: {} -> {}", 
                        deleted, before, after);
            } else {
                log.debug("[TokenCleanup] Refresh Tokens: No expired tokens to delete.");
            }
        } catch (Exception e) {
            log.error("[TokenCleanup] Error cleaning up refresh tokens: {}", e.getMessage(), e);
        }
    }

    /**
     * Xóa blacklisted tokens đã quá hạn - chạy mỗi giờ.
     * Token trong blacklist chỉ cần giữ đến khi token gốc hết hạn.
     * Cron: 0 30 * * * * (phút 30 mỗi giờ)
     */
    @Scheduled(cron = "0 30 * * * *")
    @Transactional
    public void cleanupExpiredBlacklistedTokens() {
        try {
            long before = tokenBlacklistRepository.count();
            int deleted = tokenBlacklistRepository.deleteExpiredBlacklistedTokens(Instant.now());
            long after = tokenBlacklistRepository.count();
            
            if (deleted > 0) {
                log.info("[TokenCleanup] Blacklist: Removed {} entries (tokens already expired). Total: {} -> {}", 
                        deleted, before, after);
            } else {
                log.debug("[TokenCleanup] Blacklist: No expired entries to remove.");
            }
        } catch (Exception e) {
            log.error("[TokenCleanup] Error cleaning up blacklist: {}", e.getMessage(), e);
        }
    }

    /**
     * Xóa password reset tokens hết hạn - chạy mỗi 30 phút.
     * Password reset tokens có thời gian sống ngắn (15 phút).
     * Cron: 0 0/30 * * * * (mỗi 30 phút)
     */
    @Scheduled(cron = "0 0/30 * * * *")
    @Transactional
    public void cleanupExpiredPasswordResetTokens() {
        try {
            long before = passwordResetTokenRepository.count();
            int deleted = passwordResetTokenRepository.deleteExpiredTokens(Instant.now());
            long after = passwordResetTokenRepository.count();
            
            if (deleted > 0) {
                log.info("[TokenCleanup] Password Reset Tokens: Deleted {} expired tokens. Total: {} -> {}", 
                        deleted, before, after);
            } else {
                log.debug("[TokenCleanup] Password Reset Tokens: No expired tokens to delete.");
            }
        } catch (Exception e) {
            log.error("[TokenCleanup] Error cleaning up password reset tokens: {}", e.getMessage(), e);
        }
    }

    /**
     * Xóa persistent logins (remember me) hết hạn - chạy hàng ngày lúc 3:00 AM.
     * Persistent logins thường có thời gian sống dài (30 ngày).
     * Cron: 0 0 3 * * * (3:00 AM mỗi ngày)
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupExpiredPersistentLogins() {
        try {
            long before = persistentLoginRepository.count();
            int deleted = persistentLoginRepository.deleteExpiredTokens(Instant.now());
            long after = persistentLoginRepository.count();
            
            if (deleted > 0) {
                log.info("[TokenCleanup] Persistent Logins: Deleted {} expired sessions. Total: {} -> {}", 
                        deleted, before, after);
            } else {
                log.debug("[TokenCleanup] Persistent Logins: No expired sessions to delete.");
            }
        } catch (Exception e) {
            log.error("[TokenCleanup] Error cleaning up persistent logins: {}", e.getMessage(), e);
        }
    }

    /**
     * Xóa revoked refresh tokens cũ - chạy hàng ngày lúc 4:00 AM.
     * Giữ revoked tokens trong 7 ngày để audit, sau đó xóa.
     * Cron: 0 0 4 * * * (4:00 AM mỗi ngày)
     */
    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public void cleanupOldRevokedTokens() {
        try {
            // Xóa revoked tokens cũ hơn 7 ngày
            Instant cutoffDate = Instant.now().minusSeconds(7 * 24 * 60 * 60); // 7 days ago
            
            long before = refreshTokenRepository.count();
            int deleted = refreshTokenRepository.deleteRevokedTokensOlderThan(cutoffDate);
            long after = refreshTokenRepository.count();
            
            if (deleted > 0) {
                log.info("[TokenCleanup] Old Revoked Tokens: Deleted {} tokens older than 7 days. Total: {} -> {}", 
                        deleted, before, after);
            } else {
                log.debug("[TokenCleanup] Old Revoked Tokens: No old revoked tokens to delete.");
            }
        } catch (Exception e) {
            log.error("[TokenCleanup] Error cleaning up old revoked tokens: {}", e.getMessage(), e);
        }
    }

    /**
     * Báo cáo thống kê token - chạy hàng ngày lúc 0:00.
     * Cron: 0 0 0 * * * (0:00 AM mỗi ngày)
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void dailyTokenStatistics() {
        try {
            long activeRefreshTokens = refreshTokenRepository.countActiveTokens(Instant.now());
            long revokedRefreshTokens = refreshTokenRepository.countByRevokedTrue();
            long blacklistedTokens = tokenBlacklistRepository.count();
            long pendingPasswordResets = passwordResetTokenRepository.countByUsedFalseAndExpiryDateAfter(Instant.now());
            long activePersistentLogins = persistentLoginRepository.countActiveLogins(Instant.now());
            
            log.info("[TokenCleanup] Daily Statistics at {}:", LocalDateTime.now().format(FORMATTER));
            log.info("  - Active Refresh Tokens: {}", activeRefreshTokens);
            log.info("  - Revoked Refresh Tokens: {}", revokedRefreshTokens);
            log.info("  - Blacklisted Tokens: {}", blacklistedTokens);
            log.info("  - Pending Password Resets: {}", pendingPasswordResets);
            log.info("  - Active Remember-Me Sessions: {}", activePersistentLogins);
        } catch (Exception e) {
            log.error("[TokenCleanup] Error generating statistics: {}", e.getMessage(), e);
        }
    }

    /**
     * Force cleanup - có thể gọi manual khi cần.
     */
    @Transactional
    public void forceCleanupAll() {
        log.info("[TokenCleanup] Force cleanup initiated...");
        cleanupExpiredRefreshTokens();
        cleanupExpiredBlacklistedTokens();
        cleanupExpiredPasswordResetTokens();
        cleanupExpiredPersistentLogins();
        cleanupOldRevokedTokens();
        log.info("[TokenCleanup] Force cleanup completed.");
    }
}
