package fit.hutech.BuiBaoHan.entities;

import java.time.LocalDateTime;
import java.util.Objects;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity đại diện cho Thành viên trong phòng chat
 */
@Entity
@Table(name = "blog_chat_participant", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"room_id", "user_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlogChatParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Builder.Default
    @Column(name = "is_admin")
    private Boolean isAdmin = false;

    @Builder.Default
    @Column(name = "is_muted")
    private Boolean isMuted = false;

    @Column(name = "nickname", length = 50)
    private String nickname;

    @Column(name = "last_read_at")
    private LocalDateTime lastReadAt;

    @Builder.Default
    @Column(name = "unread_count")
    private Integer unreadCount = 0;

    @CreationTimestamp
    @Column(name = "joined_at", updatable = false)
    private LocalDateTime joinedAt;

    @Column(name = "left_at")
    private LocalDateTime leftAt;

    // Quan hệ với Room
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private BlogChatRoom room;

    // Quan hệ với User
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Đánh dấu đã đọc tin nhắn
     */
    public void markAsRead() {
        this.lastReadAt = LocalDateTime.now();
        this.unreadCount = 0;
    }

    /**
     * Tăng số tin nhắn chưa đọc
     */
    public void incrementUnread() {
        this.unreadCount++;
    }

    /**
     * Rời phòng
     */
    public void leave() {
        this.leftAt = LocalDateTime.now();
    }

    /**
     * Kiểm tra đã rời phòng chưa
     */
    public boolean hasLeft() {
        return leftAt != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BlogChatParticipant that = (BlogChatParticipant) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "BlogChatParticipant{" +
                "id=" + id +
                '}';
    }
}
