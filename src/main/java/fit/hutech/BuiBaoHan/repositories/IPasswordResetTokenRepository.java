package fit.hutech.BuiBaoHan.repositories;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fit.hutech.BuiBaoHan.entities.PasswordResetToken;
import fit.hutech.BuiBaoHan.entities.User;

/**
 * Repository cho PasswordResetToken entity
 */
@Repository
public interface IPasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    /**
     * Tìm token theo token string
     */
    Optional<PasswordResetToken> findByToken(String token);

    /**
     * Tìm token valid theo token string
     */
    @Query("SELECT prt FROM PasswordResetToken prt WHERE prt.token = :token AND prt.used = false AND prt.invalidated = false AND prt.expiryDate > :now")
    Optional<PasswordResetToken> findValidToken(@Param("token") String token, @Param("now") Instant now);

    /**
     * Tìm tất cả tokens của một user
     */
    List<PasswordResetToken> findByUser(User user);

    /**
     * Tìm tokens chưa sử dụng của một user
     */
    List<PasswordResetToken> findByUserAndUsedFalse(User user);

    /**
     * Tìm token theo email
     */
    Optional<PasswordResetToken> findByEmailAndUsedFalseAndInvalidatedFalse(String email);

    /**
     * Invalidate tất cả tokens cũ của user trước khi tạo mới
     */
    @Modifying
    @Query("UPDATE PasswordResetToken prt SET prt.invalidated = true WHERE prt.user.id = :userId AND prt.used = false")
    int invalidateAllByUserId(@Param("userId") Long userId);

    /**
     * Xóa các tokens đã hết hạn (cleanup job)
     */
    @Modifying
    @Query("DELETE FROM PasswordResetToken prt WHERE prt.expiryDate < :now")
    int deleteExpiredTokens(@Param("now") Instant now);

    /**
     * Đếm số lần yêu cầu reset password từ một email trong khoảng thời gian
     * Dùng cho rate limiting
     */
    @Query("SELECT COUNT(prt) FROM PasswordResetToken prt WHERE prt.email = :email AND prt.createdAt > :since")
    long countRecentRequestsByEmail(@Param("email") String email, @Param("since") Instant since);

    /**
     * Đếm số lần yêu cầu reset password từ một IP trong khoảng thời gian
     * Dùng cho rate limiting
     */
    @Query("SELECT COUNT(prt) FROM PasswordResetToken prt WHERE prt.requestIp = :ip AND prt.createdAt > :since")
    long countRecentRequestsByIp(@Param("ip") String ip, @Param("since") Instant since);

    /**
     * Kiểm tra email có token pending không
     */
    @Query("SELECT COUNT(prt) > 0 FROM PasswordResetToken prt WHERE prt.email = :email AND prt.used = false AND prt.invalidated = false AND prt.expiryDate > :now")
    boolean hasPendingToken(@Param("email") String email, @Param("now") Instant now);
    
    /**
     * Đếm số token pending chưa sử dụng và chưa hết hạn
     */
    long countByUsedFalseAndExpiryDateAfter(Instant now);
}
