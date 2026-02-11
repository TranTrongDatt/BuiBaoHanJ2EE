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
 * Entity lưu trữ Refresh Token để hỗ trợ Token Rotation Strategy.
 * - Mỗi user có thể có nhiều refresh tokens (đa thiết bị)
 * - Token được rotate mỗi lần sử dụng
 * - Hỗ trợ phát hiện token bị đánh cắp
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "refresh_token", indexes = {
    @Index(name = "idx_refresh_token_token", columnList = "token"),
    @Index(name = "idx_refresh_token_user", columnList = "user_id"),
    @Index(name = "idx_refresh_token_expiry", columnList = "expiry_date")
})
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Token string (UUID hoặc JWT)
     */
    @Column(name = "token", nullable = false, unique = true, length = 500)
    private String token;

    /**
     * User sở hữu token này
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Thời điểm token hết hạn
     */
    @Column(name = "expiry_date", nullable = false)
    private Instant expiryDate;

    /**
     * Thời điểm token được tạo
     */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /**
     * Token đã bị thu hồi hay chưa
     * - true: đã bị revoke (do logout, password change, hoặc phát hiện bị đánh cắp)
     * - false: còn hiệu lực
     */
    @Column(name = "revoked", nullable = false)
    @Builder.Default
    private boolean revoked = false;

    /**
     * Lý do thu hồi token (nếu có)
     */
    @Column(name = "revoke_reason", length = 100)
    private String revokeReason;

    /**
     * Device/Browser information để tracking
     */
    @Column(name = "device_info", length = 255)
    private String deviceInfo;

    /**
     * IP address khi tạo token
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * Family ID để detect token reuse attack
     * Tất cả tokens trong cùng family sẽ bị revoke nếu phát hiện reuse
     */
    @Column(name = "token_family", length = 36)
    private String tokenFamily;

    /**
     * Kiểm tra token đã hết hạn chưa
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiryDate);
    }

    /**
     * Kiểm tra token còn valid không
     */
    public boolean isValid() {
        return !revoked && !isExpired();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RefreshToken that)) return false;
        if (Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
