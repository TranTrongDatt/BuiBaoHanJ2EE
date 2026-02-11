package fit.hutech.BuiBaoHan.controllers;

import java.security.Principal;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import fit.hutech.BuiBaoHan.dto.ChatMessageDto;
import fit.hutech.BuiBaoHan.entities.ChatMessage;
import fit.hutech.BuiBaoHan.services.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller xử lý WebSocket messages cho chat
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketChatController {

    private final ChatService chatService;

    // ==================== REST API ====================

    /**
     * API lấy lịch sử chat của phòng
     */
    @GetMapping("/api/chat/history/{roomId}")
    @ResponseBody
    public ResponseEntity<List<ChatMessageDto>> getChatHistory(@PathVariable String roomId) {
        return ResponseEntity.ok(chatService.getRoomHistory(roomId));
    }

    /**
     * API lấy tin nhắn private
     */
    @GetMapping("/api/chat/private")
    @ResponseBody
    public ResponseEntity<List<ChatMessageDto>> getPrivateMessages(
            @RequestParam String user1,
            @RequestParam String user2) {
        return ResponseEntity.ok(chatService.getPrivateMessages(user1, user2));
    }

    /**
     * API đếm tin nhắn chưa đọc
     */
    @GetMapping("/api/chat/unread")
    @ResponseBody
    public ResponseEntity<Long> getUnreadCount(Principal principal) {
        if (principal == null) {
            return ResponseEntity.ok(0L);
        }
        return ResponseEntity.ok(chatService.countUnreadMessages(principal.getName()));
    }

    // ==================== WEBSOCKET HANDLERS ====================

    /**
     * Xử lý tin nhắn gửi đến phòng chat
     * Client gửi đến: /app/chat/{roomId}
     * Server broadcast đến: /topic/chat/{roomId}
     */
    @MessageMapping("/chat/{roomId}")
    @SendTo("/topic/chat/{roomId}")
    public ChatMessageDto sendMessage(
            @DestinationVariable String roomId,
            @Payload ChatMessageDto messageDto,
            Principal principal) {
        
        String username = principal != null ? principal.getName() : messageDto.getSender();
        log.info("Message from {} to room {}: {}", username, roomId, messageDto.getContent());
        
        return chatService.sendPublicMessage(roomId, username, messageDto.getContent());
    }

    /**
     * Xử lý tin nhắn private
     * Client gửi đến: /app/chat/private
     * Server gửi đến: /user/{username}/queue/private
     */
    @MessageMapping("/chat/private")
    @SendToUser("/queue/private")
    public ChatMessageDto sendPrivateMessage(
            @Payload ChatMessageDto messageDto,
            Principal principal) {
        
        String senderUsername = principal != null ? principal.getName() : messageDto.getSender();
        log.info("Private message from {} to {}: {}", 
                senderUsername, messageDto.getRecipient(), messageDto.getContent());
        
        return chatService.sendPrivateMessage(
                senderUsername, 
                messageDto.getRecipient(), 
                messageDto.getContent());
    }

    /**
     * Thông báo user tham gia phòng chat
     * Client gửi đến: /app/chat/{roomId}/join
     */
    @MessageMapping("/chat/{roomId}/join")
    @SendTo("/topic/chat/{roomId}")
    public ChatMessageDto userJoin(
            @DestinationVariable String roomId,
            @Payload ChatMessageDto messageDto,
            SimpMessageHeaderAccessor headerAccessor,
            Principal principal) {
        
        String username = principal != null ? principal.getName() : messageDto.getSender();
        
        // Lưu username vào session
        headerAccessor.getSessionAttributes().put("username", username);
        headerAccessor.getSessionAttributes().put("roomId", roomId);
        
        log.info("User {} joined room {}", username, roomId);
        
        chatService.notifyUserJoinLeave(roomId, username, true);
        
        return ChatMessageDto.builder()
                .type(ChatMessage.MessageType.JOIN)
                .sender(username)
                .content(username + " đã tham gia phòng chat")
                .roomId(roomId)
                .build();
    }

    /**
     * Thông báo user rời phòng chat
     * Client gửi đến: /app/chat/{roomId}/leave
     */
    @MessageMapping("/chat/{roomId}/leave")
    @SendTo("/topic/chat/{roomId}")
    public ChatMessageDto userLeave(
            @DestinationVariable String roomId,
            @Payload ChatMessageDto messageDto,
            Principal principal) {
        
        String username = principal != null ? principal.getName() : messageDto.getSender();
        log.info("User {} left room {}", username, roomId);
        
        return ChatMessageDto.builder()
                .type(ChatMessage.MessageType.LEAVE)
                .sender(username)
                .content(username + " đã rời phòng chat")
                .roomId(roomId)
                .build();
    }

    /**
     * Thông báo user đang nhập
     * Client gửi đến: /app/chat/{roomId}/typing
     */
    @MessageMapping("/chat/{roomId}/typing")
    @SendTo("/topic/chat/{roomId}/typing")
    public ChatMessageDto userTyping(
            @DestinationVariable String roomId,
            @Payload ChatMessageDto messageDto,
            Principal principal) {
        
        String username = principal != null ? principal.getName() : messageDto.getSender();
        
        return ChatMessageDto.builder()
                .type(ChatMessage.MessageType.TYPING)
                .sender(username)
                .roomId(roomId)
                .build();
    }
}
