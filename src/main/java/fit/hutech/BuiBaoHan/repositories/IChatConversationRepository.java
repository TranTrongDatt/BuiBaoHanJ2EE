package fit.hutech.BuiBaoHan.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fit.hutech.BuiBaoHan.entities.ChatConversation;

/**
 * Repository cho ChatConversation entity
 */
@Repository
public interface IChatConversationRepository extends JpaRepository<ChatConversation, Long> {

    Optional<ChatConversation> findByConversationId(String conversationId);

    boolean existsByConversationId(String conversationId);

    List<ChatConversation> findByUserId(Long userId);

    Page<ChatConversation> findByUserId(Long userId, Pageable pageable);

    @Query("SELECT cc FROM ChatConversation cc WHERE cc.user.id = :userId AND cc.isActive = true " +
           "ORDER BY cc.lastMessageAt DESC")
    List<ChatConversation> findActiveByUserId(@Param("userId") Long userId);

    @Query("SELECT cc FROM ChatConversation cc WHERE cc.user.id = :userId " +
           "ORDER BY cc.lastMessageAt DESC NULLS LAST")
    Page<ChatConversation> findByUserIdOrderByLastMessageAtDesc(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT cc FROM ChatConversation cc LEFT JOIN FETCH cc.messages WHERE cc.id = :id")
    Optional<ChatConversation> findByIdWithMessages(@Param("id") Long id);

    @Query("SELECT COUNT(cc) FROM ChatConversation cc WHERE cc.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);

    void deleteByUserId(Long userId);
}
