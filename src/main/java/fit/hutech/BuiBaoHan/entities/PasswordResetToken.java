package fit.hutech.BuiBaoHan.entities;

import java.time.Instant;
import java.util.Objects;

import org.hibernate.Hibernate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity cho Password Reset Token.
 * 
 * Security Features:
 * - Token được mã hóa bằng AES + RSA hybrid encryption
 * - Thời hạn ngắn (15 phút) để giảm risk
 * - Chỉ sử dụng được 1 lần
 * - Liên kết với email cụ thể
 * 
 * Flow:
 * 1. User yêu cầu reset password với email
 * 2. Hệ thống tạo token, encrypt và gửi qua email
 * 3. User nhập token vào trang reset password
 * 4. Hệ thống validate token và cho phép đổi password
 * 5. Token bị đánh dấu đã sử dụng
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "password_reset_token", indexes = {
    @Index(name = "idx_password_reset_token", columnList = "token"),
    @Index(name = "idx_password_reset_user", columnList = "user_id"),
    @Index(name = "idx_password_reset_expiry", columnList = "expiry_date")
})
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Token đã được mã hóa (AES + RSA)
     * Gửi cho user qua email
     */
    @Column(name = "token", nullable = false, unique = true, length = 500)
    private String token;

    /**
     * User yêu cầu reset password
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Thời điểm token hết hạn (15 phút từ lúc tạo)
     */
    @Column(name = "expiry_date", nullable = false)
    private Instant expiryDate;

    /**
     * Thời điểm token được tạo
     */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /**
     * Token đã được sử dụng chưa (chỉ dùng 1 lần)
     */
    @Column(name = "used", nullable = false)
    @Builder.Default
    private boolean used = false;

    /**
     * Thời điểm token được sử dụng (nếu đã dùng)
     */
    @Column(name = "used_at")
    private Instant usedAt;

    /**
     * IP address khi yêu cầu reset
     */
    @Column(name = "request_ip", length = 45)
    private String requestIp;

    /**
     * IP address khi sử dụng token để reset
     */
    @Column(name = "reset_ip", length = 45)
    private String resetIp;

    /**
     * Email được gửi token (để verify)
     */
    @Column(name = "email", nullable = false, length = 100)
    private String email;

    /**
     * Số lần thử nhập token sai
     * Sau 5 lần sai -> invalidate token
     */
    @Column(name = "attempt_count", nullable = false)
    @Builder.Default
    private int attemptCount = 0;

    /**
     * Đánh dấu token đã bị invalidate do quá nhiều lần thử sai
     */
    @Column(name = "invalidated", nullable = false)
    @Builder.Default
    private boolean invalidated = false;

    /**
     * Kiểm tra token còn hiệu lực không
     */
    public boolean isValid() {
        return !used && !invalidated && !isExpired() && attemptCount < 5;
    }

    /**
     * Kiểm tra token đã hết hạn chưa
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiryDate);
    }

    /**
     * Tăng số lần thử sai
     * Trả về true nếu còn có thể thử tiếp
     */
    public boolean incrementAttempt() {
        attemptCount++;
        if (attemptCount >= 5) {
            invalidated = true;
            return false;
        }
        return true;
    }

    /**
     * Đánh dấu token đã được sử dụng
     */
    public void markAsUsed(String resetFromIp) {
        this.used = true;
        this.usedAt = Instant.now();
        this.resetIp = resetFromIp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PasswordResetToken that)) return false;
        if (Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
