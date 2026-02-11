package fit.hutech.BuiBaoHan.repositories;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fit.hutech.BuiBaoHan.dto.DashboardStatsResponse;
import fit.hutech.BuiBaoHan.entities.User;

@Repository
public interface IUserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByUsername(String username);
    
    Optional<User> findByEmail(String email);
    
    boolean existsByUsername(String username);
    
    boolean existsByEmail(String email);
    
    boolean existsByEmailIgnoreCase(String email);
    
    boolean existsByUsernameIgnoreCase(String username);

    /**
     * Find all admin users (for notifications)
     */
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = 'ROLE_ADMIN' OR r.name = 'ADMIN'")
    List<User> findAdminUsers();
    
    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<User> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
    
    long countByIsActiveTrue();
    
    long countByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    @Query(value = "SELECT * FROM user ORDER BY created_at DESC LIMIT :limit", nativeQuery = true)
    List<User> findRecentUsers(@Param("limit") int limit);
    
    // ==================== Dashboard methods ====================
    
    /**
     * Count active users (alias for countByIsActiveTrue)
     */
    default long countActiveUsers() {
        return countByIsActiveTrue();
    }
    
    /**
     * Count users created today
     */
    @Query("SELECT COUNT(u) FROM User u WHERE DATE(u.createdAt) = CURRENT_DATE")
    long countNewUsersToday();
    
    /**
     * Find top 5 recent users
     */
    List<User> findTop5ByOrderByCreatedAtDesc();
    
    /**
     * Get user registration by days
     */
    @Query(value = "SELECT DATE(created_at) as label, COUNT(*) as value " +
                   "FROM users WHERE created_at >= DATE_SUB(CURRENT_DATE, INTERVAL :days DAY) " +
                   "GROUP BY DATE(created_at) ORDER BY DATE(created_at)", nativeQuery = true)
    List<Object[]> getUserRegistrationByDaysRaw(@Param("days") int days);
    
    /**
     * Get user registration chart data
     */
    default List<DashboardStatsResponse.ChartData> getUserRegistrationByDays(int days) {
        return getUserRegistrationByDaysRaw(days).stream()
                .map(arr -> new DashboardStatsResponse.ChartData(
                        arr[0].toString(),
                        ((Number) arr[1]).doubleValue()
                ))
                .toList();
    }
    
    /**
     * Get user report for period
     */
    default Map<String, Object> getUserReport(LocalDate startDate, LocalDate endDate) {
        long newUsers = countByCreatedAtBetween(
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay()
        );
        return Map.of(
                "totalUsers", count(),
                "activeUsers", countActiveUsers(),
                "newUsers", newUsers,
                "startDate", startDate,
                "endDate", endDate
        );
    }
}