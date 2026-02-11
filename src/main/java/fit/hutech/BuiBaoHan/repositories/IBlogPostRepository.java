package fit.hutech.BuiBaoHan.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fit.hutech.BuiBaoHan.constants.PostStatus;
import fit.hutech.BuiBaoHan.constants.Visibility;
import fit.hutech.BuiBaoHan.entities.BlogPost;

/**
 * Repository cho BlogPost entity
 */
@Repository
public interface IBlogPostRepository extends JpaRepository<BlogPost, Long> {

    Optional<BlogPost> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<BlogPost> findByAuthorId(Long authorId);

    Page<BlogPost> findByAuthorId(Long authorId, Pageable pageable);

    Page<BlogPost> findByStatus(PostStatus status, Pageable pageable);

    Page<BlogPost> findByVisibility(Visibility visibility, Pageable pageable);

    @Query("SELECT bp FROM BlogPost bp WHERE bp.status = 'ACTIVE' AND bp.visibility = 'PUBLIC' " +
           "ORDER BY bp.createdAt DESC")
    Page<BlogPost> findPublicActivePosts(Pageable pageable);

    @Query("SELECT bp FROM BlogPost bp WHERE bp.book.id = :bookId AND bp.status = 'ACTIVE'")
    List<BlogPost> findByBookId(@Param("bookId") Long bookId);

    @Query("SELECT bp FROM BlogPost bp WHERE bp.isPinned = true AND bp.status = 'ACTIVE' " +
           "ORDER BY bp.createdAt DESC")
    List<BlogPost> findPinnedPosts();

    @Query("SELECT bp FROM BlogPost bp WHERE bp.status = 'ACTIVE' AND bp.visibility = 'PUBLIC' " +
           "AND (LOWER(bp.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(bp.content) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<BlogPost> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT bp FROM BlogPost bp LEFT JOIN FETCH bp.comments WHERE bp.id = :id")
    Optional<BlogPost> findByIdWithComments(@Param("id") Long id);

    @Modifying
    @Query("UPDATE BlogPost bp SET bp.viewCount = bp.viewCount + 1 WHERE bp.id = :id")
    void incrementViewCount(@Param("id") Long id);

    @Query("SELECT bp FROM BlogPost bp WHERE bp.status = 'ACTIVE' AND bp.visibility = 'PUBLIC' " +
           "ORDER BY bp.viewCount DESC")
    Page<BlogPost> findMostViewedPosts(Pageable pageable);

    @Query("SELECT bp FROM BlogPost bp WHERE bp.status = 'ACTIVE' AND bp.visibility = 'PUBLIC' " +
           "ORDER BY bp.likeCount DESC")
    Page<BlogPost> findMostLikedPosts(Pageable pageable);

    @Query("SELECT COUNT(bp) FROM BlogPost bp WHERE bp.author.id = :authorId")
    long countByAuthorId(@Param("authorId") Long authorId);

    // Additional methods for BlogPostService
    Page<BlogPost> findByStatusAndVisibility(PostStatus status, Visibility visibility, Pageable pageable);

    Page<BlogPost> findByAuthorIdAndStatusAndVisibility(Long authorId, PostStatus status, Visibility visibility, Pageable pageable);

    @Query("SELECT bp FROM BlogPost bp WHERE bp.status = 'PUBLISHED' AND bp.visibility = 'PUBLIC' " +
           "ORDER BY bp.viewCount DESC LIMIT :limit")
    List<BlogPost> findTrendingPosts(@Param("limit") int limit);

    @Query("SELECT bp FROM BlogPost bp WHERE bp.status = 'PUBLISHED' AND bp.visibility = 'PUBLIC' " +
           "ORDER BY bp.createdAt DESC LIMIT :limit")
    List<BlogPost> findLatestPosts(@Param("limit") int limit);

    // Additional methods for wrapper compatibility
    Page<BlogPost> findByIsPinnedTrue(Pageable pageable);

    Page<BlogPost> findByBookIdAndIdNot(Long bookId, Long postId, Pageable pageable);

    Page<BlogPost> findByAuthorIdAndStatus(Long authorId, PostStatus status, Pageable pageable);

    Page<BlogPost> findByAuthorUsernameAndStatusAndVisibility(
            String username, PostStatus status, Visibility visibility, Pageable pageable);
}
