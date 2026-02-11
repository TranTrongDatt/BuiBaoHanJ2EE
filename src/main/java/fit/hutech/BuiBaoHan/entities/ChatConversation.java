package fit.hutech.BuiBaoHan.entities;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Entity đại diện cho Cuộc hội thoại với AI Chatbot
 */
@Entity
@Table(name = "chat_conversation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_id", length = 50, unique = true, nullable = false)
    private String conversationId;

    @Column(name = "title", length = 255)
    private String title;

    @Builder.Default
    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @Builder.Default
    @Column(name = "message_count")
    private Integer messageCount = 0;

    // Context của cuộc hội thoại (JSON format để lưu lịch sử ngắn gọn)
    @Column(name = "context", columnDefinition = "LONGTEXT")
    private String context;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Quan hệ với User
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Danh sách tin nhắn
    @Builder.Default
    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AIChatMessage> messages = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (conversationId == null || conversationId.isBlank()) {
            conversationId = generateConversationId();
        }
    }

    /**
     * Generate conversation ID
     */
    private String generateConversationId() {
        return "CONV-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    // Helper methods
    public void addMessage(AIChatMessage message) {
        messages.add(message);
        message.setConversation(this);
        this.messageCount++;
        this.lastMessageAt = LocalDateTime.now();
    }

    public void end() {
        this.isActive = false;
    }

    public void updateTitle(String newTitle) {
        this.title = newTitle;
    }

    /**
     * Lấy tin nhắn cuối cùng
     */
    public AIChatMessage getLastMessage() {
        if (messages.isEmpty()) {
            return null;
        }
        return messages.get(messages.size() - 1);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatConversation that = (ChatConversation) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "ChatConversation{" +
                "id=" + id +
                ", conversationId='" + conversationId + '\'' +
                ", title='" + title + '\'' +
                '}';
    }
}
