package fit.hutech.BuiBaoHan.dto;

import java.time.LocalDateTime;

import fit.hutech.BuiBaoHan.entities.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO cho tin nhắn chat qua WebSocket
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageDto {

    private Long id;
    private String content;
    private String sender;        // Username của người gửi
    private String senderName;    // Tên hiển thị của người gửi
    private String recipient;     // Username người nhận (nếu private)
    private String roomId;        // ID phòng chat
    private ChatMessage.MessageType type;
    private LocalDateTime timestamp;

    /**
     * Convert từ Entity sang DTO
     */
    public static ChatMessageDto fromEntity(ChatMessage entity) {
        return ChatMessageDto.builder()
                .id(entity.getId())
                .content(entity.getContent())
                .sender(entity.getSender().getUsername())
                .senderName(entity.getSender().getUsername()) // Use username as display name
                .recipient(entity.getRecipient() != null 
                        ? entity.getRecipient().getUsername() 
                        : null)
                .roomId(entity.getRoomId())
                .type(entity.getType())
                .timestamp(entity.getCreatedAt())
                .build();
    }

    /**
     * Convert từ Entity sang DTO (alias method cho BlogChatService)
     */
    public static ChatMessageDto from(ChatMessage message) {
        ChatMessageDtoBuilder builder = ChatMessageDto.builder()
                .id(message.getId())
                .content(message.getContent())
                .sender(message.getSender().getUsername())
                .senderName(message.getSender().getFullName() != null 
                        ? message.getSender().getFullName()
                        : message.getSender().getUsername())
                .timestamp(message.getCreatedAt());

        if (message.getRecipient() != null) {
            builder.recipient(message.getRecipient().getUsername());
        }

        if (message.getRoom() != null) {
            builder.roomId(message.getRoom().getId().toString());
        } else if (message.getRoomId() != null) {
            builder.roomId(message.getRoomId());
        }

        if (message.getType() != null) {
            builder.type(message.getType());
        }

        return builder.build();
    }
}
