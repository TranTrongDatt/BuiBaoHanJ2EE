package fit.hutech.BuiBaoHan.repositories;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fit.hutech.BuiBaoHan.entities.PersistentLogin;
import fit.hutech.BuiBaoHan.entities.User;

/**
 * Repository cho PersistentLogin entity (Remember Me)
 */
@Repository
public interface IPersistentLoginRepository extends JpaRepository<PersistentLogin, String> {

    /**
     * Tìm persistent login theo series (primary key)
     */
    Optional<PersistentLogin> findBySeries(String series);

    /**
     * Tìm tất cả persistent logins của một user
     */
    List<PersistentLogin> findByUser(User user);

    /**
     * Tìm persistent login còn valid của user
     */
    @Query("SELECT pl FROM PersistentLogin pl WHERE pl.user = :user AND pl.invalidated = false AND pl.expiryDate > :now")
    List<PersistentLogin> findValidLoginsByUser(@Param("user") User user, @Param("now") Instant now);

    /**
     * Invalidate tất cả persistent logins của một user
     * Dùng khi: password change, logout all devices
     */
    @Modifying
    @Query("UPDATE PersistentLogin pl SET pl.invalidated = true, pl.invalidateReason = :reason WHERE pl.user.id = :userId")
    int invalidateAllByUserId(@Param("userId") Long userId, @Param("reason") String reason);

    /**
     * Xóa các persistent logins đã hết hạn (cleanup job)
     */
    @Modifying
    @Query("DELETE FROM PersistentLogin pl WHERE pl.expiryDate < :now")
    int deleteExpiredLogins(@Param("now") Instant now);

    /**
     * Xóa tất cả persistent logins của một user
     */
    @Modifying
    @Query("DELETE FROM PersistentLogin pl WHERE pl.user.id = :userId")
    int deleteByUserId(@Param("userId") Long userId);

    /**
     * Đếm số sessions active của một user
     */
    @Query("SELECT COUNT(pl) FROM PersistentLogin pl WHERE pl.user.id = :userId AND pl.invalidated = false AND pl.expiryDate > :now")
    long countActiveSessionsByUserId(@Param("userId") Long userId, @Param("now") Instant now);

    /**
     * Tìm theo IP address (để detect suspicious activity)
     */
    List<PersistentLogin> findByIpAddressAndInvalidatedFalse(String ipAddress);

    /**
     * Kiểm tra series và token (validation)
     */
    @Query("SELECT pl FROM PersistentLogin pl WHERE pl.series = :series AND pl.token = :token AND pl.invalidated = false")
    Optional<PersistentLogin> findBySeriesAndToken(@Param("series") String series, @Param("token") String token);
    
    /**
     * Xóa các persistent logins đã hết hạn (alias for cleanup job)
     */
    @Modifying
    @Query("DELETE FROM PersistentLogin pl WHERE pl.expiryDate < :now")
    int deleteExpiredTokens(@Param("now") Instant now);
    
    /**
     * Đếm tổng số sessions active
     */
    @Query("SELECT COUNT(pl) FROM PersistentLogin pl WHERE pl.invalidated = false AND pl.expiryDate > :now")
    long countActiveLogins(@Param("now") Instant now);
}
