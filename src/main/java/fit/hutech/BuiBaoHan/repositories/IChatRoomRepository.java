package fit.hutech.BuiBaoHan.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fit.hutech.BuiBaoHan.entities.ChatRoom;

/**
 * Repository cho ChatRoom
 */
@Repository
public interface IChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    /**
     * Tìm room theo room code
     */
    Optional<ChatRoom> findByRoomCode(String roomCode);

    /**
     * Tìm private room giữa 2 users
     */
    @Query("""
            SELECT r FROM ChatRoom r
            JOIN r.members m1
            JOIN r.members m2
            WHERE r.isPrivate = true
            AND m1.id = :userId1
            AND m2.id = :userId2
            AND SIZE(r.members) = 2
            """)
    Optional<ChatRoom> findPrivateRoom(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    /**
     * Lấy danh sách room của user
     */
    @Query("""
            SELECT r FROM ChatRoom r
            JOIN r.members m
            WHERE m.id = :userId
            ORDER BY r.updatedAt DESC
            """)
    List<ChatRoom> findByMemberId(@Param("userId") Long userId);

    /**
     * Kiểm tra user có trong room không
     */
    @Query("""
            SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
            FROM ChatRoom r
            JOIN r.members m
            WHERE r.id = :roomId AND m.id = :userId
            """)
    boolean existsByIdAndMemberId(@Param("roomId") Long roomId, @Param("userId") Long userId);
}
