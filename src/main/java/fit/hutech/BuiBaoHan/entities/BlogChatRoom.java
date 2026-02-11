package fit.hutech.BuiBaoHan.entities;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import fit.hutech.BuiBaoHan.constants.RoomType;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity đại diện cho Phòng chat của Blog
 */
@Entity
@Table(name = "blog_chat_room")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlogChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_code", length = 50, unique = true, nullable = false)
    private String roomCode;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "avatar", length = 255)
    private String avatar;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "room_type", length = 20)
    private RoomType roomType = RoomType.PRIVATE;

    @Builder.Default
    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @Builder.Default
    @Column(name = "member_count")
    private Integer memberCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Người tạo phòng
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    // Danh sách thành viên
    @Builder.Default
    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BlogChatParticipant> participants = new ArrayList<>();

    // Danh sách tin nhắn
    @Builder.Default
    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BlogChatMessage> messages = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (roomCode == null || roomCode.isBlank()) {
            roomCode = generateRoomCode();
        }
    }

    /**
     * Generate room code
     */
    private String generateRoomCode() {
        return "ROOM-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    // Helper methods
    public void addParticipant(BlogChatParticipant participant) {
        participants.add(participant);
        participant.setRoom(this);
        this.memberCount++;
    }

    public void removeParticipant(BlogChatParticipant participant) {
        participants.remove(participant);
        participant.setRoom(null);
        if (this.memberCount > 0) {
            this.memberCount--;
        }
    }

    public void addMessage(BlogChatMessage message) {
        messages.add(message);
        message.setRoom(this);
        this.lastMessageAt = LocalDateTime.now();
    }

    public boolean isPrivate() {
        return roomType == RoomType.PRIVATE;
    }

    public boolean isGroup() {
        return roomType == RoomType.GROUP;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BlogChatRoom that = (BlogChatRoom) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "BlogChatRoom{" +
                "id=" + id +
                ", roomCode='" + roomCode + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
