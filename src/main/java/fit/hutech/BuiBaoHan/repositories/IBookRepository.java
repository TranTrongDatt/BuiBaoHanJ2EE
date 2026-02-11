package fit.hutech.BuiBaoHan.repositories;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fit.hutech.BuiBaoHan.entities.Book;

@Repository
public interface IBookRepository extends JpaRepository<Book, Long> {

    /**
     * Tìm book theo ID với eager fetch author, publisher, category
     * Sử dụng cho trang edit để tránh LazyInitializationException
     */
    @Query("""
        SELECT b FROM Book b
        LEFT JOIN FETCH b.author
        LEFT JOIN FETCH b.publisher
        LEFT JOIN FETCH b.category
        WHERE b.id = :id
        """)
    Optional<Book> findWithDetailsById(@Param("id") Long id);

    /**
     * Phân trang books với eager fetch category để tránh N+1 queries
     * Sử dụng cho trang admin list books
     */
    @EntityGraph(attributePaths = {"category", "author"})
    @Query("SELECT b FROM Book b")
    Page<Book> findAllWithDetails(Pageable pageable);

    @Query("""
 SELECT b FROM Book b
 WHERE b.title LIKE %?1%
 OR b.author.name LIKE %?1%
 OR b.category.name LIKE %?1%
 """)
    List<Book> searchBook(String keyword);

    /**
     * Search books với JOIN FETCH để eager load Author và Category
     * Dùng cho AI Chatbot để tránh LazyInitializationException
     */
    @Query("""
 SELECT DISTINCT b FROM Book b
 LEFT JOIN FETCH b.author
 LEFT JOIN FETCH b.category
 WHERE LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
 OR LOWER(b.author.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
 OR LOWER(b.category.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
 """)
    List<Book> searchBookWithDetails(@Param("keyword") String keyword);

    /**
     * Find top selling books với JOIN FETCH để eager load Author và Category
     * Dùng cho AI Chatbot fallback khi không tìm thấy kết quả
     */
    @Query("""
 SELECT b FROM Book b
 LEFT JOIN FETCH b.author
 LEFT JOIN FETCH b.category
 ORDER BY b.soldCount DESC
 """)
    List<Book> findTopSellingWithDetails(Pageable pageable);

    /**
     * Lấy TẤT CẢ sách trong 1 category với JOIN FETCH
     * Dùng cho AI Chatbot: khi user hỏi về danh mục cụ thể → lấy đúng sách trong danh mục đó
     */
    @Query("""
 SELECT DISTINCT b FROM Book b
 LEFT JOIN FETCH b.author
 LEFT JOIN FETCH b.category
 WHERE b.category.id = :categoryId
 ORDER BY b.soldCount DESC
 """)
    List<Book> findByCategoryIdWithDetails(@Param("categoryId") Long categoryId);

    /**
     * Lấy TẤT CẢ sách trong 1 lĩnh vực (field) với JOIN FETCH
     * Dùng cho AI Chatbot: khi user hỏi về lĩnh vực chung → lấy tất cả sách thuộc các category của field đó
     */
    @Query("""
 SELECT DISTINCT b FROM Book b
 LEFT JOIN FETCH b.author
 LEFT JOIN FETCH b.category c
 WHERE c.field.id = :fieldId
 ORDER BY b.soldCount DESC
 """)
    List<Book> findByFieldIdWithDetails(@Param("fieldId") Long fieldId);

    default Page<Book> findAllBooks(Integer pageNo, Integer pageSize, String sortBy) {
        Sort sort = sortBy != null && !sortBy.isEmpty() ? Sort.by(sortBy) : Sort.unsorted();
        Pageable pageable = Pageable.ofSize(pageSize).withPage(pageNo);
        if (!sort.isUnsorted()) {
            pageable = org.springframework.data.domain.PageRequest.of(pageNo, pageSize, sort);
        }
        return findAllWithDetails(pageable);
    }
    
    // Report methods
    @Query("SELECT COALESCE(SUM(b.stockQuantity), 0) FROM Book b")
    Long sumLibraryStock();
    
    @Query("SELECT COUNT(b) FROM Book b WHERE b.stockQuantity > 0")
    long countAvailableBooks();
    
    @Query("SELECT b FROM Book b WHERE b.stockQuantity < :threshold ORDER BY b.stockQuantity ASC")
    List<Book> findLowLibraryStock(@Param("threshold") int threshold);
    
    @Query("SELECT b.category.name, COUNT(b) FROM Book b GROUP BY b.category.name")
    List<Object[]> countByField();

    // Additional methods for BookService
    @EntityGraph(attributePaths = {"author", "publisher", "category"})
    Optional<Book> findBySlug(String slug);

    Optional<Book> findByIsbn(String isbn);

    boolean existsBySlug(String slug);

    boolean existsByIsbn(String isbn);

    @EntityGraph(attributePaths = {"author", "category"})
    Page<Book> findByCategoryId(Long categoryId, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "category"})
    Page<Book> findByAuthorId(Long authorId, Pageable pageable);

    Page<Book> findByPublisherId(Long publisherId, Pageable pageable);

    @Query("SELECT b FROM Book b WHERE b.category.id = :fieldId")
    Page<Book> findByFieldId(@Param("fieldId") Long fieldId, Pageable pageable);

    Page<Book> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "category"})
    @Query("SELECT b FROM Book b WHERE " +
           "(:keyword IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:categoryId IS NULL OR b.category.id = :categoryId) AND " +
           "(:authorId IS NULL OR b.author.id = :authorId) AND " +
           "(:publisherId IS NULL OR b.publisher.id = :publisherId) AND " +
           "(:minPrice IS NULL OR b.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR b.price <= :maxPrice)")
    Page<Book> advancedSearch(@Param("keyword") String keyword,
                              @Param("categoryId") Long categoryId,
                              @Param("authorId") Long authorId,
                              @Param("publisherId") Long publisherId,
                              @Param("minPrice") BigDecimal minPrice,
                              @Param("maxPrice") BigDecimal maxPrice,
                              Pageable pageable);

    @EntityGraph(attributePaths = {"author", "category"})
    @Query("SELECT b FROM Book b ORDER BY b.soldCount DESC")
    Page<Book> findTopSelling(Pageable pageable);

    @EntityGraph(attributePaths = {"author", "category"})
    @Query("SELECT b FROM Book b WHERE b.author.id = :authorId AND b.id <> :excludeId")
    Page<Book> findByAuthorIdExcluding(@Param("authorId") Long authorId, 
                                        @Param("excludeId") Long excludeId, 
                                        Pageable pageable);

    @EntityGraph(attributePaths = {"author", "category"})
    @Query("SELECT b FROM Book b WHERE b.category.id = :categoryId AND b.id <> :excludeId ORDER BY b.viewCount DESC")
    List<Book> findRelatedBooks(@Param("categoryId") Long categoryId, 
                                @Param("excludeId") Long excludeId, 
                                Pageable pageable);

    long countByCategoryId(Long categoryId);

    List<Book> findByStockQuantityLessThan(int threshold);

    @Modifying
    @Query("UPDATE Book b SET b.viewCount = b.viewCount + 1 WHERE b.id = :id")
    void incrementViewCount(@Param("id") Long id);

    // LibraryController support methods
    @Query("SELECT b FROM Book b WHERE " +
           "(:keyword IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(b.author.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:categoryId IS NULL OR b.category.id = :categoryId) AND " +
           "b.stockQuantity > 0")
    Page<Book> findAvailableForLibrary(@Param("keyword") String keyword,
                                        @Param("categoryId") Long categoryId,
                                        Pageable pageable);
    
    // ==================== Dashboard methods ====================
    
    /**
     * Count books with low stock
     */
    @Query("SELECT COUNT(b) FROM Book b WHERE b.stockQuantity < :threshold")
    long countLowStock(@Param("threshold") int threshold);

    /**
     * Find books by status
     */
    @EntityGraph(attributePaths = {"author", "category"})
    Page<Book> findByStatus(fit.hutech.BuiBaoHan.constants.BookStatus status, Pageable pageable);

    /**
     * Find featured books (isFeatured = true) for homepage
     */
    @EntityGraph(attributePaths = {"author", "category"})
    @Query("SELECT b FROM Book b WHERE b.isFeatured = true AND b.status <> 'DISCONTINUED' ORDER BY b.soldCount DESC")
    List<Book> findFeaturedBooks(Pageable pageable);

    /**
     * Find top products by stock quantity for dashboard chart
     * Returns [bookTitle, stockQuantity]
     */
    @Query("SELECT b.title, b.stockQuantity FROM Book b WHERE b.stockQuantity > 0 ORDER BY b.stockQuantity DESC")
    List<Object[]> findTopByStockQuantity(Pageable pageable);
}
