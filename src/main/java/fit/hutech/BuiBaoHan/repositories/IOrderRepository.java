package fit.hutech.BuiBaoHan.repositories;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fit.hutech.BuiBaoHan.constants.OrderStatus;
import fit.hutech.BuiBaoHan.constants.PaymentStatus;
import fit.hutech.BuiBaoHan.entities.Order;

/**
 * Repository cho Order entity
 */
@Repository
public interface IOrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderCode(String orderCode);

    boolean existsByOrderCode(String orderCode);

    List<Order> findByUserId(Long userId);

    Page<Order> findByUserId(Long userId, Pageable pageable);

    List<Order> findByStatus(OrderStatus status);

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"items"})
    @Query("SELECT o FROM Order o WHERE o.user.id = :userId AND o.status = :status ORDER BY o.createdAt DESC")
    List<Order> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") OrderStatus status);

    // ==================== Admin queries with eager user loading ====================

    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT o FROM Order o")
    Page<Order> findAllWithUser(Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT o FROM Order o WHERE o.status = :status")
    Page<Order> findByStatusWithUser(@Param("status") OrderStatus status, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.paymentStatus = :paymentStatus")
    List<Order> findByPaymentStatus(@Param("paymentStatus") PaymentStatus paymentStatus);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items LEFT JOIN FETCH o.user WHERE o.id = :id")
    Optional<Order> findByIdWithItems(@Param("id") Long id);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.user WHERE o.orderCode = :orderCode")
    Optional<Order> findByOrderCodeWithUser(@Param("orderCode") String orderCode);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items LEFT JOIN FETCH o.user WHERE o.orderCode = :orderCode")
    Optional<Order> findByOrderCodeWithItems(@Param("orderCode") String orderCode);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status")
    long countByStatus(@Param("status") OrderStatus status);

    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.status = 'COMPLETED' OR o.status = 'DELIVERED'")
    BigDecimal getTotalRevenue();

    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE (o.status = 'COMPLETED' OR o.status = 'DELIVERED') " +
           "AND o.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal getRevenueBetween(@Param("startDate") LocalDateTime startDate, 
                                  @Param("endDate") LocalDateTime endDate);

    @Query("SELECT o FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate")
    List<Order> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, 
                                        @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);

    @EntityGraph(attributePaths = {"items"})
    @Query("SELECT o FROM Order o WHERE o.user.id = :userId ORDER BY o.createdAt DESC")
    Page<Order> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate AND o.status = :status")
    BigDecimal getRevenueByStatusBetween(@Param("startDate") LocalDateTime startDate, 
                                         @Param("endDate") LocalDateTime endDate,
                                         @Param("status") OrderStatus status);
    
    // ==================== Dashboard methods ====================
    
    /**
     * Count pending orders
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = 'PENDING' OR o.status = 'CONFIRMED'")
    long countPendingOrders();
    
    /**
     * Count orders by status (String version for dashboard)
     */
    default long countByStatus(String status) {
        try {
            return countByStatus(OrderStatus.valueOf(status));
        } catch (Exception e) {
            return 0L;
        }
    }
    
    /**
     * Count orders created today
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.createdAt >= CURRENT_DATE")
    long countTodayOrders();
    
    /**
     * Count orders created this week
     */
    @Query(value = "SELECT COUNT(*) FROM orders o WHERE o.created_at >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)", nativeQuery = true)
    long countWeekOrders();
    
    /**
     * Count orders created this month
     */
    @Query(value = "SELECT COUNT(*) FROM orders o WHERE YEAR(o.created_at) = YEAR(CURDATE()) AND MONTH(o.created_at) = MONTH(CURDATE())", nativeQuery = true)
    long countMonthOrders();

    /**
     * Find expired unpaid BANK_TRANSFER orders (older than given time, still PENDING payment)
     */
    @Query("SELECT o FROM Order o WHERE o.paymentMethod = 'BANK_TRANSFER' " +
           "AND o.paymentStatus = 'PENDING' " +
           "AND o.status != 'CANCELLED' " +
           "AND o.createdAt < :expirationTime")
    List<Order> findExpiredBankTransferOrders(@Param("expirationTime") LocalDateTime expirationTime);

    /**
     * Count orders by payment method for dashboard pie chart
     */
    @Query("SELECT o.paymentMethod, COUNT(o) FROM Order o WHERE o.status != 'CANCELLED' GROUP BY o.paymentMethod")
    List<Object[]> countByPaymentMethod();

    /**
     * Get today's order count (not revenue)
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.createdAt >= CURRENT_DATE")
    long getTodayOrderCount();

    /**
     * Find top customers by completed order count
     * Returns [userId, username, fullName, avatar, completedOrderCount]
     */
    @Query(value = "SELECT u.id, u.username, u.full_name, u.avatar, COUNT(o.id) as order_count " +
           "FROM orders o " +
           "JOIN user u ON o.user_id = u.id " +
           "WHERE o.status IN ('DELIVERED', 'COMPLETED') " +
           "GROUP BY u.id, u.username, u.full_name, u.avatar " +
           "HAVING COUNT(o.id) >= 1 " +
           "ORDER BY order_count DESC", nativeQuery = true)
    List<Object[]> findTopCustomersByCompletedOrders(Pageable pageable);
}
