package fit.hutech.BuiBaoHan.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fit.hutech.BuiBaoHan.entities.AIChatSession;

/**
 * Repository cho AIChatSession entity
 */
@Repository
public interface IAIChatSessionRepository extends JpaRepository<AIChatSession, Long> {

    /**
     * Tìm session đang active của user
     */
    @Query("SELECT s FROM AIChatSession s WHERE s.user.id = :userId AND s.isActive = true ORDER BY s.updatedAt DESC")
    Optional<AIChatSession> findActiveSessionByUserId(@Param("userId") Long userId);

    /**
     * Lấy tất cả sessions của user, sắp xếp theo thời gian cập nhật mới nhất
     */
    @Query("SELECT s FROM AIChatSession s WHERE s.user.id = :userId ORDER BY s.updatedAt DESC")
    List<AIChatSession> findByUserIdOrderByUpdatedAtDesc(@Param("userId") Long userId);

    /**
     * Lấy sessions active của user
     */
    @Query("SELECT s FROM AIChatSession s WHERE s.user.id = :userId AND s.isActive = true ORDER BY s.updatedAt DESC")
    List<AIChatSession> findActiveSessionsByUserId(@Param("userId") Long userId);

    /**
     * Đếm số session của user
     */
    @Query("SELECT COUNT(s) FROM AIChatSession s WHERE s.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);

    /**
     * Xóa tất cả sessions của user
     */
    void deleteByUserId(Long userId);
}
