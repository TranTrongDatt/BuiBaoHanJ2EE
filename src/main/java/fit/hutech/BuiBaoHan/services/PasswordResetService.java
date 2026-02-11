package fit.hutech.BuiBaoHan.services;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fit.hutech.BuiBaoHan.entities.PasswordResetToken;
import fit.hutech.BuiBaoHan.entities.User;
import fit.hutech.BuiBaoHan.exceptions.BadRequestException;
import fit.hutech.BuiBaoHan.exceptions.ResourceNotFoundException;
import fit.hutech.BuiBaoHan.repositories.IPasswordResetTokenRepository;
import fit.hutech.BuiBaoHan.repositories.IUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service quản lý Password Reset.
 * 
 * Flow:
 * 1. User yêu cầu reset với email
 * 2. Tạo token, encrypt và gửi email
 * 3. User nhập token để xác thực
 * 4. User đổi password mới
 * 5. Token bị invalidate
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PasswordResetService {

    private final IPasswordResetTokenRepository passwordResetTokenRepository;
    private final IUserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    @Value("${password-reset.token.expiry-minutes:15}")
    private int tokenExpiryMinutes;

    @Value("${password-reset.rate-limit.max-requests:3}")
    private int maxRequestsPerHour;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Tạo password reset token và gửi email
     */
    public void createPasswordResetToken(String email, String requestIp) {
        // Tìm user theo email
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            // Không tiết lộ email không tồn tại (security)
            log.warn("Password reset requested for non-existent email: {}", email);
            throw new ResourceNotFoundException("Nếu email tồn tại, bạn sẽ nhận được link đặt lại mật khẩu.");
        }

        // Rate limiting: max 3 requests/hour/email
        long recentRequests = passwordResetTokenRepository.countRecentRequestsByEmail(
                email, Instant.now().minus(1, ChronoUnit.HOURS));
        if (recentRequests >= maxRequestsPerHour) {
            log.warn("Rate limit exceeded for password reset: {}", email);
            throw new BadRequestException("Bạn đã yêu cầu đặt lại mật khẩu quá nhiều lần. Vui lòng thử lại sau 1 giờ.");
        }

        // Invalidate các token cũ
        passwordResetTokenRepository.invalidateAllByUserId(user.getId());

        // Tạo token mới
        String token = generateSecureToken();
        Instant now = Instant.now();
        Instant expiryDate = now.plus(tokenExpiryMinutes, ChronoUnit.MINUTES);

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .email(email)
                .expiryDate(expiryDate)
                .createdAt(now)
                .requestIp(requestIp)
                .build();

        passwordResetTokenRepository.save(resetToken);

        // Gửi email
        emailService.sendPasswordResetEmail(email, user.getUsername(), token);

        log.info("Password reset token created for user: {}", user.getUsername());
    }

    /**
     * Validate token
     */
    public boolean validateToken(String token) {
        Optional<PasswordResetToken> resetToken = 
                passwordResetTokenRepository.findValidToken(token, Instant.now());
        return resetToken.isPresent();
    }

    /**
     * Reset password với token
     */
    public void resetPassword(String token, String newPassword, String resetIp) {
        // Tìm token
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException("Token không hợp lệ hoặc đã hết hạn."));

        // Validate token
        if (!resetToken.isValid()) {
            if (resetToken.isExpired()) {
                throw new BadRequestException("Token đã hết hạn. Vui lòng yêu cầu đặt lại mật khẩu mới.");
            }
            if (resetToken.isUsed()) {
                throw new BadRequestException("Token đã được sử dụng.");
            }
            throw new BadRequestException("Token không hợp lệ.");
        }

        // Validate password
        validateNewPassword(newPassword);

        // Đổi password
        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Đánh dấu token đã sử dụng
        resetToken.markAsUsed(resetIp);
        passwordResetTokenRepository.save(resetToken);

        // Revoke tất cả refresh tokens (security: force re-login)
        refreshTokenService.revokeAllUserTokens(user.getId(), "PASSWORD_CHANGE");

        // Gửi email thông báo
        emailService.sendSecurityAlertEmail(
                user.getEmail(),
                user.getUsername(),
                "Mật khẩu đã được thay đổi",
                "Mật khẩu của bạn đã được thay đổi thành công. Nếu bạn không thực hiện hành động này, vui lòng liên hệ support ngay."
        );

        log.info("Password reset successful for user: {}", user.getUsername());
    }

    /**
     * Tăng số lần thử token sai
     */
    public boolean incrementAttempt(String token) {
        Optional<PasswordResetToken> resetTokenOpt = passwordResetTokenRepository.findByToken(token);
        if (resetTokenOpt.isPresent()) {
            PasswordResetToken resetToken = resetTokenOpt.get();
            boolean canRetry = resetToken.incrementAttempt();
            passwordResetTokenRepository.save(resetToken);
            return canRetry;
        }
        return false;
    }

    /**
     * Generate secure random token
     */
    private String generateSecureToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    /**
     * Validate new password requirements
     * - Ít nhất 6 ký tự
     * - Có chữ hoa
     * - Có chữ thường
     * - Có số
     * - Có ký tự đặc biệt
     */
    private void validateNewPassword(String password) {
        if (password == null || password.length() < 6) {
            throw new BadRequestException("Mật khẩu phải có ít nhất 6 ký tự.");
        }
        if (!password.matches(".*[A-Z].*")) {
            throw new BadRequestException("Mật khẩu phải có ít nhất 1 chữ hoa.");
        }
        if (!password.matches(".*[a-z].*")) {
            throw new BadRequestException("Mật khẩu phải có ít nhất 1 chữ thường.");
        }
        if (!password.matches(".*[0-9].*")) {
            throw new BadRequestException("Mật khẩu phải có ít nhất 1 số.");
        }
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) {
            throw new BadRequestException("Mật khẩu phải có ít nhất 1 ký tự đặc biệt.");
        }
    }

    /**
     * Cleanup expired tokens (scheduled job)
     */
    public int cleanupExpiredTokens() {
        int deleted = passwordResetTokenRepository.deleteExpiredTokens(Instant.now());
        log.info("Cleaned up {} expired password reset tokens", deleted);
        return deleted;
    }
}
