package fit.hutech.BuiBaoHan.repositories;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fit.hutech.BuiBaoHan.entities.BlogChatMessage;

/**
 * Repository cho BlogChatMessage entity
 */
@Repository
public interface IBlogChatMessageRepository extends JpaRepository<BlogChatMessage, Long> {

    List<BlogChatMessage> findByRoomId(Long roomId);

    Page<BlogChatMessage> findByRoomId(Long roomId, Pageable pageable);

    @Query("SELECT m FROM BlogChatMessage m WHERE m.room.id = :roomId AND m.isDeleted = false " +
           "ORDER BY m.createdAt DESC")
    Page<BlogChatMessage> findByRoomIdOrderByCreatedAtDesc(@Param("roomId") Long roomId, Pageable pageable);

    @Query("SELECT m FROM BlogChatMessage m WHERE m.room.id = :roomId AND m.isDeleted = false " +
           "ORDER BY m.createdAt ASC")
    List<BlogChatMessage> findByRoomIdOrderByCreatedAtAsc(@Param("roomId") Long roomId);

    @Query("SELECT m FROM BlogChatMessage m WHERE m.sender.id = :senderId ORDER BY m.createdAt DESC")
    Page<BlogChatMessage> findBySenderId(@Param("senderId") Long senderId, Pageable pageable);

    @Query("SELECT m FROM BlogChatMessage m WHERE m.room.id = :roomId " +
           "AND m.isDeleted = false ORDER BY m.createdAt DESC LIMIT 1")
    BlogChatMessage findLastMessageByRoomId(@Param("roomId") Long roomId);

    @Query("SELECT m FROM BlogChatMessage m WHERE m.replyTo.id = :messageId AND m.isDeleted = false")
    List<BlogChatMessage> findRepliesByMessageId(@Param("messageId") Long messageId);

    @Query("SELECT COUNT(m) FROM BlogChatMessage m WHERE m.room.id = :roomId AND m.isDeleted = false")
    long countByRoomId(@Param("roomId") Long roomId);

    @Query("SELECT m FROM BlogChatMessage m WHERE m.room.id = :roomId " +
           "AND m.isDeleted = false ORDER BY m.createdAt DESC")
    List<BlogChatMessage> findRecentMessages(@Param("roomId") Long roomId, Pageable pageable);

    void deleteByRoomId(Long roomId);
}
