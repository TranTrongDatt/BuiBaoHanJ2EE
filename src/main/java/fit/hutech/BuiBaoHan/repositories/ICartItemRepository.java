package fit.hutech.BuiBaoHan.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fit.hutech.BuiBaoHan.entities.CartItem;

/**
 * Repository cho CartItem entity
 */
@Repository
public interface ICartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByCartId(Long cartId);

    Optional<CartItem> findByCartIdAndBookId(Long cartId, Long bookId);

    boolean existsByCartIdAndBookId(Long cartId, Long bookId);

    void deleteByCartId(Long cartId);

    void deleteByCartIdAndBookId(Long cartId, Long bookId);

    @Query("SELECT ci FROM CartItem ci LEFT JOIN FETCH ci.book WHERE ci.cart.id = :cartId")
    List<CartItem> findByCartIdWithBooks(@Param("cartId") Long cartId);

    @Query("SELECT SUM(ci.quantity) FROM CartItem ci WHERE ci.cart.id = :cartId")
    Integer getTotalQuantityByCartId(@Param("cartId") Long cartId);

    @Query("SELECT ci FROM CartItem ci LEFT JOIN FETCH ci.book WHERE ci.cart.user.id = :userId ORDER BY ci.createdAt DESC")
    List<CartItem> findByCartUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.cart.user.id = :userId")
    void deleteByCartUserId(@Param("userId") Long userId);
}
