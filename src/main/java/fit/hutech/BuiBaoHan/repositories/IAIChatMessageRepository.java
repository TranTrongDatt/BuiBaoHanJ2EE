package fit.hutech.BuiBaoHan.repositories;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fit.hutech.BuiBaoHan.constants.SenderType;
import fit.hutech.BuiBaoHan.entities.AIChatMessage;

/**
 * Repository cho AIChatMessage entity
 */
@Repository
public interface IAIChatMessageRepository extends JpaRepository<AIChatMessage, Long> {

    List<AIChatMessage> findByConversationId(Long conversationId);

    Page<AIChatMessage> findByConversationId(Long conversationId, Pageable pageable);

    @Query("SELECT m FROM AIChatMessage m WHERE m.conversation.id = :conversationId " +
           "ORDER BY m.createdAt DESC")
    Page<AIChatMessage> findByConversationIdOrderByCreatedAtDesc(@Param("conversationId") Long conversationId, 
                                                                   Pageable pageable);

    @Query("SELECT m FROM AIChatMessage m WHERE m.conversation.id = :conversationId " +
           "ORDER BY m.createdAt ASC")
    List<AIChatMessage> findByConversationIdOrderByCreatedAtAsc(@Param("conversationId") Long conversationId);

    @Query("SELECT m FROM AIChatMessage m WHERE m.conversation.id = :conversationId " +
           "AND m.senderType = :senderType ORDER BY m.createdAt DESC")
    List<AIChatMessage> findByConversationIdAndSenderType(@Param("conversationId") Long conversationId, 
                                                           @Param("senderType") SenderType senderType);

    @Query("SELECT COUNT(m) FROM AIChatMessage m WHERE m.conversation.id = :conversationId")
    long countByConversationId(@Param("conversationId") Long conversationId);

    @Query("SELECT m FROM AIChatMessage m WHERE m.conversation.id = :conversationId " +
           "ORDER BY m.createdAt DESC LIMIT 10")
    List<AIChatMessage> findRecentMessages(@Param("conversationId") Long conversationId);

    void deleteByConversationId(Long conversationId);

    // ==================== Methods for AIChatSession ====================

    /**
     * Lấy messages theo session ID, sắp xếp theo thời gian tăng dần
     */
    @Query("SELECT m FROM AIChatMessage m WHERE m.session.id = :sessionId ORDER BY m.createdAt ASC")
    List<AIChatMessage> findBySessionIdOrderByCreatedAtAsc(@Param("sessionId") Long sessionId);

    /**
     * Lấy N messages gần nhất của session
     */
    @Query(value = "SELECT * FROM ai_chat_message m WHERE m.session_id = :sessionId ORDER BY m.created_at DESC", nativeQuery = true)
    List<AIChatMessage> findRecentBySessionId(@Param("sessionId") Long sessionId, Pageable pageable);

    /**
     * Xóa tất cả messages của session
     */
    void deleteBySessionId(Long sessionId);

    /**
     * Đếm số messages trong session
     */
    @Query("SELECT COUNT(m) FROM AIChatMessage m WHERE m.session.id = :sessionId")
    long countBySessionId(@Param("sessionId") Long sessionId);
}
