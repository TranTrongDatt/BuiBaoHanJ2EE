package fit.hutech.BuiBaoHan.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fit.hutech.BuiBaoHan.constants.RoomType;
import fit.hutech.BuiBaoHan.entities.BlogChatRoom;

/**
 * Repository cho BlogChatRoom entity
 */
@Repository
public interface IBlogChatRoomRepository extends JpaRepository<BlogChatRoom, Long> {

    Optional<BlogChatRoom> findByRoomCode(String roomCode);

    boolean existsByRoomCode(String roomCode);

    List<BlogChatRoom> findByCreatorId(Long creatorId);

    List<BlogChatRoom> findByRoomType(RoomType roomType);

    @Query("SELECT r FROM BlogChatRoom r WHERE r.isActive = true ORDER BY r.lastMessageAt DESC NULLS LAST")
    Page<BlogChatRoom> findActiveRooms(Pageable pageable);

    @Query("SELECT r FROM BlogChatRoom r JOIN r.participants p WHERE p.user.id = :userId " +
           "AND p.leftAt IS NULL AND r.isActive = true ORDER BY r.lastMessageAt DESC NULLS LAST")
    List<BlogChatRoom> findRoomsByUserId(@Param("userId") Long userId);

    @Query("SELECT r FROM BlogChatRoom r LEFT JOIN FETCH r.participants WHERE r.id = :id")
    Optional<BlogChatRoom> findByIdWithParticipants(@Param("id") Long id);

    @Query("SELECT r FROM BlogChatRoom r LEFT JOIN FETCH r.messages WHERE r.id = :id")
    Optional<BlogChatRoom> findByIdWithMessages(@Param("id") Long id);

    @Query("SELECT r FROM BlogChatRoom r WHERE r.roomType = 'PRIVATE' " +
           "AND EXISTS (SELECT p1 FROM BlogChatParticipant p1 WHERE p1.room = r AND p1.user.id = :userId1) " +
           "AND EXISTS (SELECT p2 FROM BlogChatParticipant p2 WHERE p2.room = r AND p2.user.id = :userId2)")
    Optional<BlogChatRoom> findPrivateRoomBetweenUsers(@Param("userId1") Long userId1, 
                                                        @Param("userId2") Long userId2);
}
