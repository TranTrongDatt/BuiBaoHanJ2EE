package fit.hutech.BuiBaoHan.entities;

import java.time.Instant;
import java.util.Objects;

import org.hibernate.Hibernate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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
 * Entity cho Remember Me functionality với Persistent Token Strategy.
 * 
 * Security features:
 * - Series identifier: không thay đổi trong suốt session
 * - Token: được rotate mỗi lần sử dụng
 * - Phát hiện token theft: nếu series đúng nhưng token sai = bị đánh cắp
 * 
 * Reference: https://docs.spring.io/spring-security/reference/servlet/authentication/rememberme.html
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "persistent_login", indexes = {
    @Index(name = "idx_persistent_login_user", columnList = "user_id"),
    @Index(name = "idx_persistent_login_last_used", columnList = "last_used")
})
public class PersistentLogin {

    /**
     * Series identifier - random UUID, không thay đổi trong session
     * Đây là primary key
     */
    @Id
    @Column(name = "series", length = 64)
    private String series;

    /**
     * Token value - được hash trước khi lưu
     * Token được rotate sau mỗi lần auto-login thành công
     */
    @Column(name = "token", nullable = false, length = 64)
    private String token;

    /**
     * User sở hữu persistent login này
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Thời điểm token được sử dụng/tạo gần nhất
     */
    @Column(name = "last_used", nullable = false)
    private Instant lastUsed;

    /**
     * Thời điểm hết hạn (thường là 30 ngày từ lần sử dụng cuối)
     */
    @Column(name = "expiry_date", nullable = false)
    private Instant expiryDate;

    /**
     * IP address khi tạo remember-me
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * User agent (browser info)
     */
    @Column(name = "user_agent", length = 255)
    private String userAgent;

    /**
     * Đánh dấu token đã bị thu hồi (do phát hiện theft attempt)
     */
    @Column(name = "invalidated", nullable = false)
    @Builder.Default
    private boolean invalidated = false;

    /**
     * Lý do invalidate (nếu có)
     */
    @Column(name = "invalidate_reason", length = 100)
    private String invalidateReason;

    /**
     * Kiểm tra persistent login còn hiệu lực không
     */
    public boolean isValid() {
        return !invalidated && Instant.now().isBefore(expiryDate);
    }

    /**
     * Kiểm tra đã hết hạn chưa
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiryDate);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PersistentLogin that)) return false;
        if (Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        return getSeries() != null && Objects.equals(getSeries(), that.getSeries());
    }

    @Override
    public int hashCode() {
        return Objects.hash(series);
    }
}
