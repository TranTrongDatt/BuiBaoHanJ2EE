package fit.hutech.BuiBaoHan.entities;

import java.time.Instant;
import java.util.Objects;

import org.hibernate.Hibernate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity lưu trữ các tokens đã bị blacklist.
 * Sử dụng để invalidate JWT tokens trước khi hết hạn.
 * 
 * Các trường hợp blacklist:
 * - User logout
 * - Password change
 * - Security breach detected
 * - Admin force logout
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "token_blacklist", indexes = {
    @Index(name = "idx_token_blacklist_token", columnList = "token"),
    @Index(name = "idx_token_blacklist_expiry", columnList = "expiry_date")
})
public class TokenBlacklist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * JWT token bị blacklist
     * Lưu ý: chỉ lưu phần signature hoặc hash để tiết kiệm storage
     */
    @Column(name = "token", nullable = false, unique = true, length = 500)
    private String token;

    /**
     * Thời điểm token hết hạn tự nhiên
     * Dùng để scheduled job cleanup các records cũ
     */
    @Column(name = "expiry_date", nullable = false)
    private Instant expiryDate;

    /**
     * Thời điểm token bị blacklist
     */
    @Column(name = "blacklisted_at", nullable = false)
    private Instant blacklistedAt;

    /**
     * Lý do blacklist
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "reason", length = 50)
    private BlacklistReason reason;

    /**
     * User ID liên quan (nếu có)
     */
    @Column(name = "user_id")
    private Long userId;

    /**
     * Loại token (ACCESS hoặc REFRESH)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "token_type", length = 20)
    @Builder.Default
    private TokenType tokenType = TokenType.ACCESS;

    /**
     * Enum các lý do blacklist token
     */
    public enum BlacklistReason {
        LOGOUT,              // User đăng xuất
        PASSWORD_CHANGE,     // User đổi mật khẩu
        SECURITY_BREACH,     // Phát hiện token bị đánh cắp
        ADMIN_REVOKE,        // Admin thu hồi quyền
        TOKEN_ROTATION,      // Token cũ trong rotation
        ACCOUNT_LOCKED       // Tài khoản bị khóa
    }

    /**
     * Enum loại token
     */
    public enum TokenType {
        ACCESS,
        REFRESH
    }

    /**
     * Kiểm tra record này đã có thể cleanup chưa
     * (token đã hết hạn tự nhiên)
     */
    public boolean canBeCleanedUp() {
        return Instant.now().isAfter(expiryDate);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TokenBlacklist that)) return false;
        if (Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
