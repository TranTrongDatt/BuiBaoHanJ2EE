package fit.hutech.BuiBaoHan.repositories;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fit.hutech.BuiBaoHan.entities.RefreshToken;
import fit.hutech.BuiBaoHan.entities.User;

/**
 * Repository cho RefreshToken entity
 */
@Repository
public interface IRefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * Tìm refresh token theo token string
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Tìm tất cả refresh tokens của một user
     */
    List<RefreshToken> findByUser(User user);

    /**
     * Tìm tất cả refresh tokens chưa bị revoke của một user
     */
    List<RefreshToken> findByUserAndRevokedFalse(User user);

    /**
     * Tìm tất cả tokens trong cùng một token family
     */
    List<RefreshToken> findByTokenFamily(String tokenFamily);

    /**
     * Kiểm tra token có tồn tại và còn valid không
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.token = :token AND rt.revoked = false AND rt.expiryDate > :now")
    Optional<RefreshToken> findValidToken(@Param("token") String token, @Param("now") Instant now);

    /**
     * Revoke tất cả tokens của một user
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true, rt.revokeReason = :reason WHERE rt.user.id = :userId")
    int revokeAllByUserId(@Param("userId") Long userId, @Param("reason") String reason);

    /**
     * Revoke tất cả tokens trong một token family
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true, rt.revokeReason = :reason WHERE rt.tokenFamily = :family")
    int revokeAllByTokenFamily(@Param("family") String family, @Param("reason") String reason);

    /**
     * Xóa các tokens đã hết hạn (cleanup job)
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiryDate < :now")
    int deleteExpiredTokens(@Param("now") Instant now);

    /**
     * Đếm số tokens active của một user
     */
    @Query("SELECT COUNT(rt) FROM RefreshToken rt WHERE rt.user.id = :userId AND rt.revoked = false AND rt.expiryDate > :now")
    long countActiveTokensByUserId(@Param("userId") Long userId, @Param("now") Instant now);

    /**
     * Tìm token theo user và device info
     */
    Optional<RefreshToken> findByUserAndDeviceInfoAndRevokedFalse(User user, String deviceInfo);
    
    /**
     * Đếm tổng số tokens active
     */
    @Query("SELECT COUNT(rt) FROM RefreshToken rt WHERE rt.revoked = false AND rt.expiryDate > :now")
    long countActiveTokens(@Param("now") Instant now);
    
    /**
     * Đếm số tokens đã bị revoke
     */
    long countByRevokedTrue();
    
    /**
     * Xóa revoked tokens cũ hơn một thời điểm
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.revoked = true AND rt.createdAt < :cutoffDate")
    int deleteRevokedTokensOlderThan(@Param("cutoffDate") Instant cutoffDate);
}
