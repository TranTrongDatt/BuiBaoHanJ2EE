package fit.hutech.BuiBaoHan.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fit.hutech.BuiBaoHan.entities.BlogChatParticipant;

/**
 * Repository cho BlogChatParticipant entity
 */
@Repository
public interface IBlogChatParticipantRepository extends JpaRepository<BlogChatParticipant, Long> {

    Optional<BlogChatParticipant> findByRoomIdAndUserId(Long roomId, Long userId);

    boolean existsByRoomIdAndUserId(Long roomId, Long userId);

    List<BlogChatParticipant> findByRoomId(Long roomId);

    List<BlogChatParticipant> findByUserId(Long userId);

    @Query("SELECT p FROM BlogChatParticipant p WHERE p.room.id = :roomId AND p.leftAt IS NULL")
    List<BlogChatParticipant> findActiveParticipantsByRoomId(@Param("roomId") Long roomId);

    @Query("SELECT p FROM BlogChatParticipant p WHERE p.room.id = :roomId AND p.isAdmin = true")
    List<BlogChatParticipant> findAdminsByRoomId(@Param("roomId") Long roomId);

    @Query("SELECT COUNT(p) FROM BlogChatParticipant p WHERE p.room.id = :roomId AND p.leftAt IS NULL")
    int countActiveParticipantsByRoomId(@Param("roomId") Long roomId);

    @Modifying
    @Query("UPDATE BlogChatParticipant p SET p.unreadCount = p.unreadCount + 1 " +
           "WHERE p.room.id = :roomId AND p.user.id != :senderId AND p.leftAt IS NULL")
    void incrementUnreadCountForRoom(@Param("roomId") Long roomId, @Param("senderId") Long senderId);

    @Modifying
    @Query("UPDATE BlogChatParticipant p SET p.unreadCount = 0, p.lastReadAt = CURRENT_TIMESTAMP " +
           "WHERE p.room.id = :roomId AND p.user.id = :userId")
    void markAsRead(@Param("roomId") Long roomId, @Param("userId") Long userId);

    void deleteByRoomIdAndUserId(Long roomId, Long userId);
}
