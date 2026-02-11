package fit.hutech.BuiBaoHan.entities;

import java.time.LocalDateTime;
import java.util.Objects;

import org.hibernate.annotations.CreationTimestamp;

import fit.hutech.BuiBaoHan.constants.MessageType;
import fit.hutech.BuiBaoHan.constants.SenderType;
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
 * Entity đại diện cho Tin nhắn trong cuộc hội thoại AI
 */
@Entity
@Table(name = "ai_chat_message")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Nội dung tin nhắn không được để trống")
    @Column(name = "content", columnDefinition = "LONGTEXT", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender_type", length = 20, nullable = false)
    private SenderType senderType;

    // Thời gian xử lý tin nhắn (ms) - hữu ích cho việc theo dõi performance
    @Column(name = "processing_time")
    private Long processingTime;

    // Số token sử dụng (nếu có)
    @Column(name = "token_count")
    private Integer tokenCount;

    // Model AI được sử dụng
    @Column(name = "model", length = 50)
    private String model;

    // Intent được detect (nếu có)
    @Column(name = "intent", length = 100)
    private String intent;

    // Confidence score của AI response
    @Column(name = "confidence")
    private Double confidence;

    @Builder.Default
    @Column(name = "is_error")
    private Boolean isError = false;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Quan hệ với ChatConversation
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id")
    private ChatConversation conversation;

    // Quan hệ với AIChatSession (cho AI chat service)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private AIChatSession session;

    // Role của message (user, assistant, system)
    @Column(name = "role", length = 20)
    private String role;

    // Loại tin nhắn
    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", length = 20)
    private MessageType messageType;

    // Feedback từ user về message
    @Column(name = "is_helpful")
    private Boolean isHelpful;

    @Column(name = "feedback", length = 1000)
    private String feedback;

    /**
     * Kiểm tra tin nhắn từ người dùng
     */
    public boolean isFromUser() {
        return senderType == SenderType.USER;
    }

    /**
     * Kiểm tra tin nhắn từ AI
     */
    public boolean isFromAI() {
        return senderType == SenderType.AI;
    }

    /**
     * Kiểm tra tin nhắn từ hệ thống
     */
    public boolean isFromSystem() {
        return senderType == SenderType.SYSTEM;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AIChatMessage that = (AIChatMessage) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "AIChatMessage{" +
                "id=" + id +
                ", senderType=" + senderType +
                '}';
    }
}
