package fit.hutech.BuiBaoHan.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fit.hutech.BuiBaoHan.constants.MessageType;
import fit.hutech.BuiBaoHan.dto.ChatMessageDto;
import fit.hutech.BuiBaoHan.entities.ChatMessage;
import fit.hutech.BuiBaoHan.entities.ChatRoom;
import fit.hutech.BuiBaoHan.entities.User;
import fit.hutech.BuiBaoHan.repositories.IChatMessageRepository;
import fit.hutech.BuiBaoHan.repositories.IChatRoomRepository;
import fit.hutech.BuiBaoHan.repositories.IUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service quản lý Blog Chat (Chat giữa users)
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BlogChatService {

    private final IChatRoomRepository chatRoomRepository;
    private final IChatMessageRepository chatMessageRepository;
    private final IUserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // ==================== Chat Room Management ====================

    /**
     * Lấy hoặc tạo private chat room giữa 2 users
     */
    public ChatRoom getOrCreatePrivateRoom(Long userId1, Long userId2) {
        // Đảm bảo userId1 < userId2 để tránh tạo duplicate room
        Long smaller = Math.min(userId1, userId2);
        Long larger = Math.max(userId1, userId2);

        Optional<ChatRoom> existingRoom = chatRoomRepository.findPrivateRoom(smaller, larger);
        
        if (existingRoom.isPresent()) {
            return existingRoom.get();
        }

        User user1 = userRepository.findById(smaller)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy user ID: " + smaller));
        User user2 = userRepository.findById(larger)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy user ID: " + larger));

        ChatRoom room = ChatRoom.builder()
                .roomCode(generateRoomCode())
                .name("Chat với " + user2.getFullName())
                .isPrivate(true)
                .createdBy(user1)
                .createdAt(LocalDateTime.now())
                .build();

        room.getMembers().add(user1);
        room.getMembers().add(user2);

        ChatRoom saved = chatRoomRepository.save(room);
        log.info("Created private chat room {} between users {} and {}", saved.getRoomCode(), smaller, larger);
        return saved;
    }

    /**
     * Tạo group chat room
     */
    public ChatRoom createGroupRoom(Long creatorId, String roomName, List<Long> memberIds) {
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy user ID: " + creatorId));

        ChatRoom room = ChatRoom.builder()
                .roomCode(generateRoomCode())
                .name(roomName)
                .isPrivate(false)
                .createdBy(creator)
                .createdAt(LocalDateTime.now())
                .build();

        room.getMembers().add(creator);

        for (Long memberId : memberIds) {
            User member = userRepository.findById(memberId).orElse(null);
            if (member != null && !member.getId().equals(creatorId)) {
                room.getMembers().add(member);
            }
        }

        ChatRoom saved = chatRoomRepository.save(room);
        log.info("Created group chat room {} with {} members", saved.getRoomCode(), room.getMembers().size());
        return saved;
    }

    /**
     * Lấy room theo ID
     */
    @Transactional(readOnly = true)
    public Optional<ChatRoom> getRoomById(Long roomId) {
        return chatRoomRepository.findById(roomId);
    }

    /**
     * Lấy room theo code
     */
    @Transactional(readOnly = true)
    public Optional<ChatRoom> getRoomByCode(String roomCode) {
        return chatRoomRepository.findByRoomCode(roomCode);
    }

    /**
     * Lấy danh sách rooms của user
     */
    @Transactional(readOnly = true)
    public List<ChatRoom> getUserRooms(Long userId) {
        return chatRoomRepository.findByMemberId(userId);
    }

    /**
     * Lấy danh sách rooms của user (wrapper cho User object)
     */
    @Transactional(readOnly = true)
    public List<ChatRoom> getUserRooms(User user) {
        return getUserRooms(user.getId());
    }

    /**
     * Đếm tổng số tin nhắn chưa đọc của user (wrapper)
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(User user) {
        return countTotalUnread(user.getId());
    }

    /**
     * Gửi tin nhắn (wrapper đơn giản cho User object)
     */
    public ChatMessage sendMessage(Long roomId, User user, String content) {
        return sendMessage(roomId, user.getId(), content, MessageType.TEXT);
    }

    /**
     * Xóa phòng chat
     */
    public void deleteRoom(Long roomId, User user) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy room ID: " + roomId));

        if (!isUserInRoom(user.getId(), roomId)) {
            throw new IllegalArgumentException("Bạn không có quyền xóa room này");
        }

        // Only creator can delete or admin
        if (!room.getCreatedBy().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Chỉ người tạo room mới có thể xóa");
        }

        chatMessageRepository.deleteByRoomId(roomId);
        chatRoomRepository.delete(room);
        log.info("Deleted chat room {} by user {}", roomId, user.getId());
    }

    /**
     * Block user trong chat
     */
    public void blockUser(User user, Long blockedUserId) {
        if (user.getId().equals(blockedUserId)) {
            throw new IllegalArgumentException("Không thể block chính mình");
        }
        
        // Verify user exists
        if (!userRepository.existsById(blockedUserId)) {
            throw new IllegalArgumentException("Không tìm thấy user ID: " + blockedUserId);
        }
        
        // TODO: Implement blocked users table/relationship
        log.info("User {} blocked user {}", user.getId(), blockedUserId);
    }

    /**
     * Unblock user trong chat
     */
    public void unblockUser(User user, Long unblockedUserId) {
        // Verify user exists
        if (!userRepository.existsById(unblockedUserId)) {
            throw new IllegalArgumentException("Không tìm thấy user ID: " + unblockedUserId);
        }
        
        // TODO: Implement blocked users table/relationship 
        log.info("User {} unblocked user {}", user.getId(), unblockedUserId);
    }

    /**
     * Lấy room theo ID và kiểm tra quyền truy cập
     */
    @Transactional(readOnly = true)
    public Optional<ChatRoom> getRoomById(Long roomId, User user) {
        Optional<ChatRoom> room = getRoomById(roomId);
        if (room.isPresent() && isUserInRoom(user.getId(), roomId)) {
            return room;
        }
        return Optional.empty();
    }

    /**
     * Đánh dấu đã đọc (wrapper cho User object)
     */
    public void markAsRead(Long roomId, User user) {
        markMessagesAsRead(roomId, user.getId());
    }

    /**
     * Lấy messages của room (wrapper cho User object)
     */
    @Transactional(readOnly = true)
    public Page<ChatMessage> getRoomMessages(Long roomId, User user, Pageable pageable) {
        return getRoomMessages(roomId, user.getId(), pageable);
    }

    /**
     * Lấy hoặc tạo room giữa 2 users
     */
    public ChatRoom getOrCreateRoom(User currentUser, Long otherUserId) {
        return getOrCreatePrivateRoom(currentUser.getId(), otherUserId);
    }

    /**
     * Kiểm tra user có trong room không
     */
    @Transactional(readOnly = true)
    public boolean isUserInRoom(Long userId, Long roomId) {
        return chatRoomRepository.existsByIdAndMemberId(roomId, userId);
    }

    /**
     * Thêm member vào room
     */
    public void addMemberToRoom(Long roomId, Long userId, Long addedByUserId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy room ID: " + roomId));

        if (room.getIsPrivate()) {
            throw new IllegalStateException("Không thể thêm thành viên vào private chat");
        }

        if (!isUserInRoom(addedByUserId, roomId)) {
            throw new IllegalArgumentException("Bạn không có quyền thêm thành viên vào room này");
        }

        User newMember = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy user ID: " + userId));

        room.getMembers().add(newMember);
        chatRoomRepository.save(room);
        log.info("Added user {} to room {}", userId, roomId);
    }

    /**
     * Rời room
     */
    public void leaveRoom(Long roomId, Long userId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy room ID: " + roomId));

        if (room.getIsPrivate()) {
            throw new IllegalStateException("Không thể rời private chat");
        }

        room.getMembers().removeIf(u -> u.getId().equals(userId));
        chatRoomRepository.save(room);
        log.info("User {} left room {}", userId, roomId);
    }

    // ==================== Messaging ====================

    /**
     * Gửi tin nhắn (wrapper cho User object và String type)
     */
    public ChatMessage sendMessage(Long roomId, Long senderId, String content, String messageTypeStr) {
        MessageType messageType = MessageType.TEXT;
        try {
            if (messageTypeStr != null) {
                messageType = MessageType.valueOf(messageTypeStr.toUpperCase());
            }
        } catch (IllegalArgumentException e) {
            log.warn("Unknown message type: {}, defaulting to TEXT", messageTypeStr);
        }
        return sendMessage(roomId, senderId, content, messageType);
    }

    /**
     * Gửi tin nhắn
     */
    public ChatMessage sendMessage(Long roomId, Long senderId, String content, MessageType messageType) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy room ID: " + roomId));

        if (!isUserInRoom(senderId, roomId)) {
            throw new IllegalArgumentException("Bạn không phải thành viên của room này");
        }

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy user ID: " + senderId));

        ChatMessage message = ChatMessage.builder()
                .room(room)
                .sender(sender)
                .content(content)
                .messageType(messageType != null ? messageType : MessageType.TEXT)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        ChatMessage saved = chatMessageRepository.save(message);

        // Cập nhật last message
        room.setLastMessage(saved);
        room.setUpdatedAt(LocalDateTime.now());
        chatRoomRepository.save(room);

        // Gửi real-time qua WebSocket
        broadcastMessage(room, saved);

        log.info("Message sent in room {} by user {}", roomId, senderId);
        return saved;
    }

    /**
     * Lấy messages của room
     */
    @Transactional(readOnly = true)
    public Page<ChatMessage> getRoomMessages(Long roomId, Long userId, Pageable pageable) {
        if (!isUserInRoom(userId, roomId)) {
            throw new IllegalArgumentException("Bạn không có quyền xem tin nhắn của room này");
        }

        return chatMessageRepository.findByRoomIdOrderByCreatedAtDesc(roomId, pageable);
    }

    /**
     * Đánh dấu đã đọc
     */
    public void markMessagesAsRead(Long roomId, Long userId) {
        if (!isUserInRoom(userId, roomId)) {
            return;
        }

        chatMessageRepository.markAsRead(roomId, userId, LocalDateTime.now());
        log.info("Marked messages as read in room {} for user {}", roomId, userId);
    }

    /**
     * Đếm tin nhắn chưa đọc
     */
    @Transactional(readOnly = true)
    public long countUnreadMessages(Long roomId, Long userId) {
        return chatMessageRepository.countUnreadByRoomAndUser(roomId, userId);
    }

    /**
     * Tổng số tin nhắn chưa đọc của user
     */
    @Transactional(readOnly = true)
    public long countTotalUnread(Long userId) {
        return chatMessageRepository.countTotalUnreadByUser(userId);
    }

    /**
     * Xóa tin nhắn
     */
    public void deleteMessage(Long messageId, Long userId) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy message ID: " + messageId));

        if (!message.getSender().getId().equals(userId)) {
            throw new IllegalArgumentException("Bạn không có quyền xóa tin nhắn này");
        }

        message.setIsDeleted(true);
        message.setContent("[Tin nhắn đã bị xóa]");
        chatMessageRepository.save(message);
        log.info("Deleted message {}", messageId);
    }

    /**
     * Chỉnh sửa tin nhắn
     */
    public ChatMessage editMessage(Long messageId, Long userId, String newContent) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy message ID: " + messageId));

        if (!message.getSender().getId().equals(userId)) {
            throw new IllegalArgumentException("Bạn không có quyền chỉnh sửa tin nhắn này");
        }

        message.setContent(newContent);
        message.setIsEdited(true);
        message.setEditedAt(LocalDateTime.now());

        ChatMessage edited = chatMessageRepository.save(message);
        log.info("Edited message {}", messageId);
        return edited;
    }

    // ==================== Private Helper Methods ====================

    private String generateRoomCode() {
        return "ROOM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private void broadcastMessage(ChatRoom room, ChatMessage message) {
        try {
            ChatMessageDto dto = ChatMessageDto.from(message);
            
            // Gửi đến tất cả members trong room
            for (User member : room.getMembers()) {
                if (!member.getId().equals(message.getSender().getId())) {
                    messagingTemplate.convertAndSendToUser(
                            member.getId().toString(),
                            "/queue/chat/" + room.getId(),
                            dto
                    );
                }
            }
        } catch (MessagingException e) {
            log.warn("Failed to broadcast message: {}", e.getMessage());
        }
    }
}
