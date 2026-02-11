package fit.hutech.BuiBaoHan.repositories;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fit.hutech.BuiBaoHan.entities.ChatMessage;
import fit.hutech.BuiBaoHan.entities.User;

/**
 * Repository cho ChatMessage
 */
@Repository
public interface IChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * Lấy tin nhắn theo phòng chat, sắp xếp theo thời gian
     */
    List<ChatMessage> findByRoomIdOrderByCreatedAtAsc(String roomId);

    /**
     * Lấy tin nhắn theo phòng chat với phân trang
     */
    Page<ChatMessage> findByRoomIdOrderByCreatedAtDesc(String roomId, Pageable pageable);

    /**
     * Lấy tin nhắn private giữa 2 user
     */
    @Query("""
            SELECT m FROM ChatMessage m 
            WHERE m.type = 'PRIVATE' 
            AND ((m.sender = :user1 AND m.recipient = :user2) 
                 OR (m.sender = :user2 AND m.recipient = :user1))
            ORDER BY m.createdAt ASC
            """)
    List<ChatMessage> findPrivateMessagesBetweenUsers(
            @Param("user1") User user1,
            @Param("user2") User user2);

    /**
     * Lấy tin nhắn chưa đọc của user
     */
    @Query("""
            SELECT m FROM ChatMessage m 
            WHERE m.recipient = :user AND m.isRead = false
            ORDER BY m.createdAt DESC
            """)
    List<ChatMessage> findUnreadMessagesByRecipient(@Param("user") User user);

    /**
     * Đếm số tin nhắn chưa đọc
     */
    @Query("""
            SELECT COUNT(m) FROM ChatMessage m 
            WHERE m.recipient = :user AND m.isRead = false
            """)
    Long countUnreadMessages(@Param("user") User user);

    /**
     * Lấy các phòng chat mà user đã tham gia
     */
    @Query("""
            SELECT DISTINCT m.roomId FROM ChatMessage m 
            WHERE m.sender = :user OR m.recipient = :user
            """)
    List<String> findRoomsByUser(@Param("user") User user);

    // ==================== Methods for BlogChatService (Long roomId) ====================

    /**
     * Lấy tin nhắn theo room ID (Long) với phân trang
     */
    @Query("""
            SELECT m FROM ChatMessage m
            WHERE m.room.id = :roomId
            ORDER BY m.createdAt DESC
            """)
    Page<ChatMessage> findByRoomIdOrderByCreatedAtDesc(@Param("roomId") Long roomId, Pageable pageable);

    /**
     * Đếm tổng số tin nhắn chưa đọc của user (trên tất cả rooms)
     */
    @Query("""
            SELECT COUNT(m) FROM ChatMessage m
            JOIN m.room r
            JOIN r.members member
            WHERE member.id = :userId
            AND m.sender.id <> :userId
            AND m.isRead = false
            """)
    long countTotalUnreadByUser(@Param("userId") Long userId);

    /**
     * Đếm số tin nhắn chưa đọc trong room của user
     */
    @Query("""
            SELECT COUNT(m) FROM ChatMessage m
            WHERE m.room.id = :roomId
            AND m.sender.id <> :userId
            AND m.isRead = false
            """)
    long countUnreadByRoomAndUser(@Param("roomId") Long roomId, @Param("userId") Long userId);

    /**
     * Đánh dấu đã đọc tất cả tin nhắn trong room cho user
     */
    @Modifying
    @Query("""
            UPDATE ChatMessage m
            SET m.isRead = true
            WHERE m.room.id = :roomId
            AND m.sender.id <> :userId
            AND m.isRead = false
            """)
    void markAsRead(@Param("roomId") Long roomId, @Param("userId") Long userId, @Param("readAt") LocalDateTime readAt);

    /**
     * Xóa tất cả tin nhắn trong room
     */
    @Modifying
    @Query("DELETE FROM ChatMessage m WHERE m.room.id = :roomId")
    void deleteByRoomId(@Param("roomId") Long roomId);
}
