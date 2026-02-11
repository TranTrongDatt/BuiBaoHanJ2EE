package fit.hutech.BuiBaoHan.services;

import java.time.LocalDateTime;

import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import fit.hutech.BuiBaoHan.constants.NotificationType;
import fit.hutech.BuiBaoHan.dto.ChatMessageDto;
import fit.hutech.BuiBaoHan.dto.NotificationDto;
import fit.hutech.BuiBaoHan.entities.ChatMessage;
import fit.hutech.BuiBaoHan.entities.ChatRoom;
import fit.hutech.BuiBaoHan.entities.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service gửi thông báo real-time cho Chat
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Gửi tin nhắn mới đến room
     */
    public void sendNewMessage(ChatRoom room, ChatMessage message) {
        try {
            ChatMessageDto dto = ChatMessageDto.from(message);
            
            // Gửi đến topic của room
            messagingTemplate.convertAndSend(
                    "/topic/chat/" + room.getId(),
                    dto
            );

            // Gửi notification đến từng member (trừ sender)
            for (User member : room.getMembers()) {
                if (!member.getId().equals(message.getSender().getId())) {
                    sendMessageNotification(member.getId(), message);
                }
            }

            log.debug("Sent chat message to room {}", room.getId());
        } catch (MessagingException e) {
            log.error("Failed to send chat message notification: {}", e.getMessage());
        }
    }

    /**
     * Gửi thông báo tin nhắn mới đến user cụ thể
     */
    public void sendMessageNotification(Long userId, ChatMessage message) {
        try {
            String senderName = message.getSender().getFullName();
            String content = message.getContent();
            if (content.length() > 50) {
                content = content.substring(0, 50) + "...";
            }

            NotificationDto notification = NotificationDto.builder()
                    .id(null)
                    .title("Tin nhắn mới từ " + senderName)
                    .content(content)
                    .type(NotificationType.CHAT)
                    .isRead(false)
                    .readAt(null)
                    .actionUrl("/chat/room/" + message.getRoom().getId())
                    .icon("💬")
                    .createdAt(LocalDateTime.now())
                    .senderId(message.getSender().getId())
                    .senderName(senderName)
                    .senderAvatar(message.getSender().getAvatar())
                    .build();

            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/chat-notifications",
                    notification
            );
        } catch (MessagingException e) {
            log.warn("Failed to send message notification to user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * Thông báo typing indicator
     */
    public void sendTypingIndicator(Long roomId, Long userId, String username, boolean isTyping) {
        try {
            TypingIndicator indicator = new TypingIndicator(userId, username, isTyping);
            
            messagingTemplate.convertAndSend(
                    "/topic/chat/" + roomId + "/typing",
                    indicator
            );
        } catch (MessagingException e) {
            log.warn("Failed to send typing indicator: {}", e.getMessage());
        }
    }

    /**
     * Thông báo online status
     */
    public void sendOnlineStatus(Long userId, boolean isOnline) {
        try {
            OnlineStatus status = new OnlineStatus(userId, isOnline, System.currentTimeMillis());
            
            messagingTemplate.convertAndSend(
                    "/topic/users/status",
                    status
            );
        } catch (MessagingException e) {
            log.warn("Failed to send online status: {}", e.getMessage());
        }
    }

    /**
     * Thông báo user tham gia room
     */
    public void sendUserJoinedRoom(Long roomId, User user) {
        try {
            RoomEvent event = new RoomEvent("USER_JOINED", user.getId(), user.getFullName(), roomId);
            
            messagingTemplate.convertAndSend(
                    "/topic/chat/" + roomId + "/events",
                    event
            );
        } catch (MessagingException e) {
            log.warn("Failed to send user joined event: {}", e.getMessage());
        }
    }

    /**
     * Thông báo user rời room
     */
    public void sendUserLeftRoom(Long roomId, User user) {
        try {
            RoomEvent event = new RoomEvent("USER_LEFT", user.getId(), user.getFullName(), roomId);
            
            messagingTemplate.convertAndSend(
                    "/topic/chat/" + roomId + "/events",
                    event
            );
        } catch (MessagingException e) {
            log.warn("Failed to send user left event: {}", e.getMessage());
        }
    }

    /**
     * Thông báo tin nhắn bị xóa
     */
    public void sendMessageDeleted(Long roomId, Long messageId) {
        try {
            MessageEvent event = new MessageEvent("MESSAGE_DELETED", messageId, roomId);
            
            messagingTemplate.convertAndSend(
                    "/topic/chat/" + roomId + "/events",
                    event
            );
        } catch (MessagingException e) {
            log.warn("Failed to send message deleted event: {}", e.getMessage());
        }
    }

    /**
     * Thông báo tin nhắn được chỉnh sửa
     */
    public void sendMessageEdited(Long roomId, ChatMessage message) {
        try {
            ChatMessageDto dto = ChatMessageDto.from(message);
            
            messagingTemplate.convertAndSend(
                    "/topic/chat/" + roomId + "/message-updated",
                    dto
            );
        } catch (MessagingException e) {
            log.warn("Failed to send message edited event: {}", e.getMessage());
        }
    }

    /**
     * Thông báo đã đọc tin nhắn
     */
    public void sendMessageRead(Long roomId, Long userId, Long lastReadMessageId) {
        try {
            ReadReceipt receipt = new ReadReceipt(userId, lastReadMessageId, System.currentTimeMillis());
            
            messagingTemplate.convertAndSend(
                    "/topic/chat/" + roomId + "/read-receipts",
                    receipt
            );
        } catch (MessagingException e) {
            log.warn("Failed to send read receipt: {}", e.getMessage());
        }
    }

    // ==================== Inner Record Classes ====================

    public record TypingIndicator(Long userId, String username, boolean isTyping) {}

    public record OnlineStatus(Long userId, boolean isOnline, long timestamp) {}

    public record RoomEvent(String type, Long userId, String username, Long roomId) {}

    public record MessageEvent(String type, Long messageId, Long roomId) {}

    public record ReadReceipt(Long userId, Long lastReadMessageId, long timestamp) {}
}
