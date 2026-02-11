package fit.hutech.BuiBaoHan.services;

import java.util.List;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fit.hutech.BuiBaoHan.config.RabbitMQConfig;
import fit.hutech.BuiBaoHan.dto.ChatMessageDto;
import fit.hutech.BuiBaoHan.entities.ChatMessage;
import fit.hutech.BuiBaoHan.entities.User;
import fit.hutech.BuiBaoHan.repositories.IChatMessageRepository;
import lombok.extern.slf4j.Slf4j;

/**
 * Service xử lý logic chat
 * - Lưu tin nhắn vào database
 * - Gửi tin nhắn qua RabbitMQ (nếu enabled)
 * - Broadcast tin nhắn qua WebSocket
 */
@Service
@Slf4j
@Transactional
public class ChatService {

    private final IChatMessageRepository chatMessageRepository;
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;
    
    // RabbitTemplate is optional - only available if rabbitmq.enabled=true
    @Autowired(required = false)
    private RabbitTemplate rabbitTemplate;
    
    @Value("${rabbitmq.enabled:false}")
    private boolean rabbitmqEnabled;

    public ChatService(IChatMessageRepository chatMessageRepository, 
                       UserService userService,
                       SimpMessagingTemplate messagingTemplate) {
        this.chatMessageRepository = chatMessageRepository;
        this.userService = userService;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Gửi tin nhắn public vào phòng chat
     */
    public ChatMessageDto sendPublicMessage(String roomId, String senderUsername, String content) {
        User sender = userService.findByUsername(senderUsername)
                .orElseThrow(() -> new RuntimeException("User not found: " + senderUsername));

        ChatMessage message = ChatMessage.builder()
                .content(content)
                .sender(sender)
                .roomId(roomId)
                .type(ChatMessage.MessageType.CHAT)
                .build();

        ChatMessage savedMessage = chatMessageRepository.save(message);
        ChatMessageDto messageDto = ChatMessageDto.fromEntity(savedMessage);

        // Gửi qua RabbitMQ nếu enabled, hoặc trực tiếp qua WebSocket
        if (rabbitmqEnabled && rabbitTemplate != null) {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.CHAT_EXCHANGE,
                    RabbitMQConfig.CHAT_ROUTING_KEY,
                    messageDto);
        } else {
            // Fallback: gửi trực tiếp qua WebSocket
            messagingTemplate.convertAndSend("/topic/chat/" + roomId, messageDto);
        }

        log.info("Message sent to room {}: {}", roomId, content);
        return messageDto;
    }

    /**
     * Gửi tin nhắn private đến user cụ thể
     */
    public ChatMessageDto sendPrivateMessage(String senderUsername, String recipientUsername, String content) {
        User sender = userService.findByUsername(senderUsername)
                .orElseThrow(() -> new RuntimeException("Sender not found: " + senderUsername));
        User recipient = userService.findByUsername(recipientUsername)
                .orElseThrow(() -> new RuntimeException("Recipient not found: " + recipientUsername));

        ChatMessage message = ChatMessage.builder()
                .content(content)
                .sender(sender)
                .recipient(recipient)
                .type(ChatMessage.MessageType.PRIVATE)
                .build();

        ChatMessage savedMessage = chatMessageRepository.save(message);
        ChatMessageDto messageDto = ChatMessageDto.fromEntity(savedMessage);

        // Gửi trực tiếp đến user qua WebSocket
        messagingTemplate.convertAndSendToUser(
                recipientUsername,
                "/queue/private",
                messageDto);

        log.info("Private message sent from {} to {}", senderUsername, recipientUsername);
        return messageDto;
    }

    /**
     * Handle broadcast message từ RabbitMQ (được gọi từ RabbitMQChatListener)
     */
    public void broadcastToRoom(ChatMessageDto messageDto) {
        log.info("Broadcasting message to room: {}", messageDto.getRoomId());
        messagingTemplate.convertAndSend(
                "/topic/chat/" + messageDto.getRoomId(),
                messageDto);
    }

    /**
     * Lấy lịch sử chat của phòng
     */
    public List<ChatMessageDto> getRoomHistory(String roomId) {
        return chatMessageRepository.findByRoomIdOrderByCreatedAtAsc(roomId)
                .stream()
                .map(ChatMessageDto::fromEntity)
                .toList();
    }

    /**
     * Lấy lịch sử chat với phân trang
     */
    public Page<ChatMessageDto> getRoomHistoryPaged(String roomId, int page, int size) {
        return chatMessageRepository
                .findByRoomIdOrderByCreatedAtDesc(roomId, PageRequest.of(page, size))
                .map(ChatMessageDto::fromEntity);
    }

    /**
     * Lấy tin nhắn private giữa 2 user
     */
    public List<ChatMessageDto> getPrivateMessages(String username1, String username2) {
        User user1 = userService.findByUsername(username1)
                .orElseThrow(() -> new RuntimeException("User not found: " + username1));
        User user2 = userService.findByUsername(username2)
                .orElseThrow(() -> new RuntimeException("User not found: " + username2));

        return chatMessageRepository.findPrivateMessagesBetweenUsers(user1, user2)
                .stream()
                .map(ChatMessageDto::fromEntity)
                .toList();
    }

    /**
     * Đánh dấu tin nhắn đã đọc
     */
    public void markAsRead(Long messageId) {
        chatMessageRepository.findById(messageId).ifPresent(message -> {
            message.setIsRead(true);
            chatMessageRepository.save(message);
        });
    }

    /**
     * Đếm số tin nhắn chưa đọc của user
     */
    public Long countUnreadMessages(String username) {
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        return chatMessageRepository.countUnreadMessages(user);
    }

    /**
     * Thông báo user join/leave room
     */
    public void notifyUserJoinLeave(String roomId, String username, boolean isJoin) {
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        ChatMessage message = ChatMessage.builder()
                .content(username + (isJoin ? " đã tham gia phòng chat" : " đã rời phòng chat"))
                .sender(user)
                .roomId(roomId)
                .type(isJoin ? ChatMessage.MessageType.JOIN : ChatMessage.MessageType.LEAVE)
                .build();

        chatMessageRepository.save(message);
        ChatMessageDto messageDto = ChatMessageDto.fromEntity(message);

        // Broadcast thông báo
        messagingTemplate.convertAndSend("/topic/chat/" + roomId, messageDto);
    }
}
