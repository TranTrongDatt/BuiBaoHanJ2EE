package fit.hutech.BuiBaoHan.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fit.hutech.BuiBaoHan.entities.Wishlist;

/**
 * Repository cho Wishlist entity
 */
@Repository
public interface IWishlistRepository extends JpaRepository<Wishlist, Long> {

    List<Wishlist> findByUserId(Long userId);

    List<Wishlist> findByUserIdOrderByCreatedAtDesc(Long userId);

    Page<Wishlist> findByUserId(Long userId, Pageable pageable);

    @Query(value = "SELECT w FROM Wishlist w LEFT JOIN FETCH w.book b LEFT JOIN FETCH b.author WHERE w.user.id = :userId ORDER BY w.createdAt DESC",
           countQuery = "SELECT COUNT(w) FROM Wishlist w WHERE w.user.id = :userId")
    Page<Wishlist> findByUserIdWithBooksAndAuthorPaged(@Param("userId") Long userId, Pageable pageable);

    Optional<Wishlist> findByUserIdAndBookId(Long userId, Long bookId);

    boolean existsByUserIdAndBookId(Long userId, Long bookId);

    void deleteByUserIdAndBookId(Long userId, Long bookId);

    void deleteByUserId(Long userId);

    @Query("SELECT w FROM Wishlist w LEFT JOIN FETCH w.book WHERE w.user.id = :userId")
    List<Wishlist> findByUserIdWithBooks(@Param("userId") Long userId);

    @Query("SELECT w FROM Wishlist w LEFT JOIN FETCH w.book b LEFT JOIN FETCH b.author WHERE w.user.id = :userId ORDER BY w.createdAt DESC")
    List<Wishlist> findByUserIdWithBooksAndAuthor(@Param("userId") Long userId);

    @Query("SELECT COUNT(w) FROM Wishlist w WHERE w.user.id = :userId")
    int countByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(w) FROM Wishlist w WHERE w.book.id = :bookId")
    int countByBookId(@Param("bookId") Long bookId);

    @Query(value = "SELECT b FROM Book b WHERE b.id IN (" +
            "SELECT w.book.id FROM Wishlist w GROUP BY w.book.id ORDER BY COUNT(w) DESC" +
            ") ORDER BY (SELECT COUNT(w2) FROM Wishlist w2 WHERE w2.book.id = b.id) DESC")
    List<fit.hutech.BuiBaoHan.entities.Book> findMostWishedBooks(@Param("limit") int limit);
}
