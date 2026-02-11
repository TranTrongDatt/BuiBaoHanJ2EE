package fit.hutech.BuiBaoHan.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fit.hutech.BuiBaoHan.entities.BlogPost;
import fit.hutech.BuiBaoHan.entities.Book;
import fit.hutech.BuiBaoHan.entities.Like;
import fit.hutech.BuiBaoHan.entities.User;

/**
 * Repository cho Like entity
 */
@Repository
public interface ILikeRepository extends JpaRepository<Like, Long> {

    // Like cho BlogPost
    Optional<Like> findByUserIdAndBlogPostId(Long userId, Long blogPostId);

    boolean existsByUserIdAndBlogPostId(Long userId, Long blogPostId);

    void deleteByUserIdAndBlogPostId(Long userId, Long blogPostId);

    // Like cho Comment
    Optional<Like> findByUserIdAndCommentId(Long userId, Long commentId);

    boolean existsByUserIdAndCommentId(Long userId, Long commentId);

    void deleteByUserIdAndCommentId(Long userId, Long commentId);

    // Like cho Book
    Optional<Like> findByUserIdAndBookId(Long userId, Long bookId);

    boolean existsByUserIdAndBookId(Long userId, Long bookId);

    void deleteByUserIdAndBookId(Long userId, Long bookId);

    // Count
    @Query("SELECT COUNT(l) FROM Like l WHERE l.blogPost.id = :blogPostId")
    long countByBlogPostId(@Param("blogPostId") Long blogPostId);

    @Query("SELECT COUNT(l) FROM Like l WHERE l.comment.id = :commentId")
    long countByCommentId(@Param("commentId") Long commentId);

    @Query("SELECT COUNT(l) FROM Like l WHERE l.book.id = :bookId")
    long countByBookId(@Param("bookId") Long bookId);

    @Query("SELECT COUNT(l) FROM Like l WHERE l.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);

    // Find users who liked a blog post
    @Query("SELECT l.user FROM Like l WHERE l.blogPost.id = :blogPostId")
    List<User> findUsersByBlogPostId(@Param("blogPostId") Long blogPostId);

    // Find most liked books
    @Query(value = "SELECT b FROM Book b LEFT JOIN Like l ON l.book.id = b.id " +
           "GROUP BY b.id ORDER BY COUNT(l) DESC")
    List<Book> findMostLikedBooks(@Param("limit") int limit);

    // Find most liked blog posts
    @Query(value = "SELECT bp FROM BlogPost bp LEFT JOIN Like l ON l.blogPost.id = bp.id " +
           "GROUP BY bp.id ORDER BY COUNT(l) DESC")
    List<BlogPost> findMostLikedBlogPosts(@Param("limit") int limit);
}
