package fit.hutech.BuiBaoHan.entities;

import java.time.LocalDateTime;
import java.util.Objects;

import org.hibernate.annotations.CreationTimestamp;

import fit.hutech.BuiBaoHan.constants.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity đại diện cho Thông báo
 */
@Entity
@Table(name = "notification")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Tiêu đề thông báo không được để trống")
    @Column(name = "title", length = 255, nullable = false)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 30)
    private NotificationType type;

    @Builder.Default
    @Column(name = "is_read")
    private Boolean isRead = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    // Link để điều hướng khi click vào thông báo
    @Column(name = "action_url", length = 255)
    private String actionUrl;

    // Icon của thông báo
    @Column(name = "icon", length = 100)
    private String icon;

    // Dữ liệu bổ sung (JSON format)
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Quan hệ với User (người nhận thông báo)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Người gửi thông báo (có thể null nếu là system)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private User sender;

    /**
     * Đánh dấu đã đọc
     */
    public void markAsRead() {
        this.isRead = true;
        this.readAt = LocalDateTime.now();
    }

    /**
     * Kiểm tra đã đọc chưa
     */
    public boolean isRead() {
        return isRead != null && isRead;
    }

    /**
     * Lấy icon dựa trên type
     */
    public String getIconOrDefault() {
        if (icon != null && !icon.isBlank()) {
            return icon;
        }
        if (type == null) {
            return "bi-bell";
        }
        switch (type) {
            case ORDER:
                return "bi-cart-check";
            case BORROW:
                return "bi-book";
            case RETURN:
                return "bi-arrow-return-left";
            case OVERDUE:
                return "bi-exclamation-triangle";
            case COMMENT:
                return "bi-chat-dots";
            case LIKE:
                return "bi-heart";
            case FOLLOW:
                return "bi-person-plus";
            case FINE:
                return "bi-cash-coin";
            case PAYMENT:
                return "bi-credit-card";
            case PROMOTION:
                return "bi-tag";
            case SYSTEM:
            default:
                return "bi-bell";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Notification that = (Notification) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Notification{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", type=" + type +
                ", isRead=" + isRead +
                '}';
    }
}
