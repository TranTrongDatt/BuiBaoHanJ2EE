package fit.hutech.BuiBaoHan.controllers.websocket;

import java.security.Principal;
import java.time.LocalDateTime;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import fit.hutech.BuiBaoHan.entities.User;
import fit.hutech.BuiBaoHan.services.BlogChatService;
import fit.hutech.BuiBaoHan.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket Controller for real-time chat
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketController {

    private final BlogChatService chatService;
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Handle sending message to a chat room
     */
    @MessageMapping("/chat.send/{roomId}")
    public void sendMessage(
            @DestinationVariable Long roomId,
            @Payload ChatMessagePayload payload,
            Principal principal) {
        
        if (principal == null) {
            log.warn("Unauthorized message attempt to room: {}", roomId);
            return;
        }
        
        try {
            User sender = userService.findByUsername(principal.getName())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            
            // Save message and broadcast
            var savedMessage = chatService.sendMessage(
                    roomId,
                    sender.getId(),
                    payload.content(),
                    payload.type() != null ? payload.type() : "TEXT"
            );
            
            // Create response
            ChatMessageResponse response = new ChatMessageResponse(
                    savedMessage.getId(),
                    roomId,
                    new UserInfo(sender.getId(), sender.getUsername(), sender.getAvatar()),
                    savedMessage.getContent(),
                    savedMessage.getMessageType() != null ? savedMessage.getMessageType().name() : "TEXT",
                    savedMessage.getCreatedAt(),
                    false
            );
            
            // Send to room subscribers
            messagingTemplate.convertAndSend("/topic/room/" + roomId, response);
            
            // Get other user in room for notification
            chatService.getRoomById(roomId, sender).ifPresent(room -> {
                User otherUser = room.getMembers().stream()
                        .filter(m -> !m.getId().equals(sender.getId()))
                        .findFirst()
                        .orElse(null);
                if (otherUser == null) return;
                
                // Send notification to specific user
                messagingTemplate.convertAndSendToUser(
                        otherUser.getUsername(),
                        "/queue/notifications",
                        new MessageNotification(
                                "NEW_MESSAGE",
                                roomId,
                                sender.getUsername(),
                                payload.content().length() > 50 
                                        ? payload.content().substring(0, 50) + "..." 
                                        : payload.content()
                        )
                );
            });
            
        } catch (Exception e) {
            log.error("Error sending message to room {}: {}", roomId, e.getMessage());
            messagingTemplate.convertAndSendToUser(
                    principal.getName(),
                    "/queue/errors",
                    new ErrorMessage("Failed to send message: " + e.getMessage())
            );
        }
    }

    /**
     * Handle typing indicator
     */
    @MessageMapping("/chat.typing/{roomId}")
    public void typingIndicator(
            @DestinationVariable Long roomId,
            @Payload TypingPayload payload,
            Principal principal) {
        
        if (principal == null) return;
        
        userService.findByUsername(principal.getName()).ifPresent(user -> {
            TypingEvent event = new TypingEvent(
                    roomId,
                    user.getId(),
                    user.getUsername(),
                    payload.isTyping()
            );
            messagingTemplate.convertAndSend("/topic/room/" + roomId + "/typing", event);
        });
    }

    /**
     * Handle read receipt
     */
    @MessageMapping("/chat.read/{roomId}")
    public void markAsRead(
            @DestinationVariable Long roomId,
            Principal principal) {
        
        if (principal == null) return;
        
        userService.findByUsername(principal.getName()).ifPresent(user -> {
            chatService.markAsRead(roomId, user);
            
            ReadReceipt receipt = new ReadReceipt(
                    roomId,
                    user.getId(),
                    LocalDateTime.now()
            );
            messagingTemplate.convertAndSend("/topic/room/" + roomId + "/read", receipt);
        });
    }

    /**
     * Handle user going online/offline
     */
    @MessageMapping("/user.status")
    @SendTo("/topic/users/status")
    public UserStatusEvent userStatus(@Payload UserStatusPayload payload, Principal principal) {
        if (principal == null) return null;
        
        return userService.findByUsername(principal.getName())
                .map(user -> {
                    userService.updateOnlineStatus(user.getId(), payload.online());
                    return new UserStatusEvent(
                            user.getId(),
                            user.getUsername(),
                            payload.online(),
                            LocalDateTime.now()
                    );
                })
                .orElse(null);
    }

    /**
     * Handle direct message without room (creates room if needed)
     */
    @MessageMapping("/chat.direct/{targetUserId}")
    @SendToUser("/queue/room-created")
    public RoomCreatedEvent sendDirectMessage(
            @DestinationVariable Long targetUserId,
            @Payload ChatMessagePayload payload,
            Principal principal) {
        
        if (principal == null) return null;
        
        try {
            User sender = userService.findByUsername(principal.getName())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            
            // Get or create room
            var room = chatService.getOrCreateRoom(sender, targetUserId);
            
            // Send message
            sendMessage(room.getId(), payload, principal);
            
            return new RoomCreatedEvent(room.getId(), targetUserId);
            
        } catch (Exception e) {
            log.error("Error sending direct message to user {}: {}", targetUserId, e.getMessage());
            return null;
        }
    }

    // ==================== Payload & Response Records ====================

    public record ChatMessagePayload(
            String content,
            String type  // TEXT, IMAGE, FILE
    ) {}

    public record ChatMessageResponse(
            Long id,
            Long roomId,
            UserInfo sender,
            String content,
            String type,
            LocalDateTime timestamp,
            boolean read
    ) {}

    public record UserInfo(
            Long id,
            String username,
            String avatarUrl
    ) {}

    public record MessageNotification(
            String type,
            Long roomId,
            String senderUsername,
            String preview
    ) {}

    public record ErrorMessage(String message) {}

    public record TypingPayload(boolean isTyping) {}

    public record TypingEvent(
            Long roomId,
            Long userId,
            String username,
            boolean isTyping
    ) {}

    public record ReadReceipt(
            Long roomId,
            Long userId,
            LocalDateTime readAt
    ) {}

    public record UserStatusPayload(boolean online) {}

    public record UserStatusEvent(
            Long userId,
            String username,
            boolean online,
            LocalDateTime timestamp
    ) {}

    public record RoomCreatedEvent(
            Long roomId,
            Long targetUserId
    ) {}
}
