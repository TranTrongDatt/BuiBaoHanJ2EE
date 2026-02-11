package fit.hutech.BuiBaoHan.repositories;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fit.hutech.BuiBaoHan.entities.TokenBlacklist;
import fit.hutech.BuiBaoHan.entities.TokenBlacklist.TokenType;

/**
 * Repository cho TokenBlacklist entity
 */
@Repository
public interface ITokenBlacklistRepository extends JpaRepository<TokenBlacklist, Long> {

    /**
     * Kiểm tra token có trong blacklist không
     */
    boolean existsByToken(String token);

    /**
     * Tìm token trong blacklist
     */
    Optional<TokenBlacklist> findByToken(String token);

    /**
     * Kiểm tra token có trong blacklist và chưa hết hạn không
     */
    @Query("SELECT COUNT(tb) > 0 FROM TokenBlacklist tb WHERE tb.token = :token AND tb.expiryDate > :now")
    boolean isTokenBlacklisted(@Param("token") String token, @Param("now") Instant now);

    /**
     * Xóa các tokens đã hết hạn (cleanup job)
     * Vì token đã hết hạn tự nhiên nên không cần giữ trong blacklist nữa
     */
    @Modifying
    @Query("DELETE FROM TokenBlacklist tb WHERE tb.expiryDate < :now")
    int deleteExpiredTokens(@Param("now") Instant now);

    /**
     * Đếm số tokens bị blacklist của một user
     */
    long countByUserId(Long userId);

    /**
     * Đếm số tokens bị blacklist theo loại
     */
    long countByTokenType(TokenType tokenType);

    /**
     * Xóa tất cả blacklist tokens của một user
     * (Dùng khi xóa user)
     */
    @Modifying
    @Query("DELETE FROM TokenBlacklist tb WHERE tb.userId = :userId")
    int deleteByUserId(@Param("userId") Long userId);

    /**
     * Blacklist tất cả tokens của một user (batch operation)
     * Dùng khi: password change, security breach, account lock
     */
    @Query("SELECT COUNT(tb) FROM TokenBlacklist tb WHERE tb.userId = :userId AND tb.blacklistedAt > :since")
    long countRecentBlacklistByUserId(@Param("userId") Long userId, @Param("since") Instant since);
    
    /**
     * Xóa các tokens đã hết hạn (alias for cleanup job)
     */
    @Modifying
    @Query("DELETE FROM TokenBlacklist tb WHERE tb.expiryDate < :now")
    int deleteExpiredBlacklistedTokens(@Param("now") Instant now);
}
