package fit.hutech.BuiBaoHan.repositories;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fit.hutech.BuiBaoHan.entities.OrderItem;

/**
 * Repository cho OrderItem entity
 */
@Repository
public interface IOrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    @Query("SELECT oi FROM OrderItem oi WHERE oi.book.id = :bookId")
    List<OrderItem> findByBookId(@Param("bookId") Long bookId);

    @Query("SELECT oi FROM OrderItem oi LEFT JOIN FETCH oi.book WHERE oi.order.id = :orderId")
    List<OrderItem> findByOrderIdWithBooks(@Param("orderId") Long orderId);

    @Query("SELECT SUM(oi.quantity) FROM OrderItem oi WHERE oi.book.id = :bookId " +
           "AND oi.order.status = 'COMPLETED'")
    Long getTotalSoldByBookId(@Param("bookId") Long bookId);

    @Query("SELECT oi.book.id, SUM(oi.quantity) as total FROM OrderItem oi " +
           "WHERE oi.order.status = 'COMPLETED' GROUP BY oi.book.id ORDER BY total DESC")
    List<Object[]> findBestSellingBooks();

    /**
     * Find best selling categories in current month
     * Returns [categoryName, totalSold]
     */
    @Query(value = "SELECT c.name, COALESCE(SUM(oi.quantity), 0) as total " +
           "FROM order_item oi " +
           "JOIN book b ON oi.book_id = b.id " +
           "JOIN category c ON b.category_id = c.id " +
           "JOIN orders o ON oi.order_id = o.id " +
           "WHERE o.status IN ('DELIVERED', 'COMPLETED') " +
           "AND YEAR(o.created_at) = YEAR(CURDATE()) " +
           "AND MONTH(o.created_at) = MONTH(CURDATE()) " +
           "GROUP BY c.id, c.name " +
           "ORDER BY total DESC", nativeQuery = true)
    List<Object[]> findBestSellingCategoriesInMonth(Pageable pageable);
}
