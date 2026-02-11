package fit.hutech.BuiBaoHan.repositories;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fit.hutech.BuiBaoHan.constants.CommentStatus;
import fit.hutech.BuiBaoHan.entities.Comment;

/**
 * Repository cho Comment entity
 */
@Repository
public interface ICommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByUserId(Long userId);

    Page<Comment> findByUserId(Long userId, Pageable pageable);

    @Query("SELECT c FROM Comment c WHERE c.blogPost.id = :blogPostId AND c.parent IS NULL " +
           "AND c.status = 'VISIBLE' ORDER BY c.createdAt DESC")
    Page<Comment> findRootCommentsByBlogPostId(@Param("blogPostId") Long blogPostId, Pageable pageable);

    @Query("SELECT c FROM Comment c WHERE c.book.id = :bookId AND c.parent IS NULL " +
           "AND c.status = 'VISIBLE' ORDER BY c.createdAt DESC")
    Page<Comment> findRootCommentsByBookId(@Param("bookId") Long bookId, Pageable pageable);

    @Query("SELECT c FROM Comment c WHERE c.parent.id = :parentId AND c.status = 'VISIBLE' " +
           "ORDER BY c.createdAt ASC")
    List<Comment> findRepliesByParentId(@Param("parentId") Long parentId);

    List<Comment> findByStatus(CommentStatus status);

    Page<Comment> findByStatus(CommentStatus status, Pageable pageable);

    @Modifying
    @Query("UPDATE Comment c SET c.likeCount = c.likeCount + 1 WHERE c.id = :id")
    void incrementLikeCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Comment c SET c.likeCount = c.likeCount - 1 WHERE c.id = :id AND c.likeCount > 0")
    void decrementLikeCount(@Param("id") Long id);

    @Query("SELECT COUNT(c) FROM Comment c WHERE c.blogPost.id = :blogPostId AND c.status = 'VISIBLE'")
    long countByBlogPostId(@Param("blogPostId") Long blogPostId);

    @Query("SELECT COUNT(c) FROM Comment c WHERE c.book.id = :bookId AND c.status = 'VISIBLE'")
    long countByBookId(@Param("bookId") Long bookId);

    @Query("SELECT c FROM Comment c WHERE c.status = 'REPORTED'")
    Page<Comment> findReportedComments(Pageable pageable);

    // Additional methods for CommentService
    Page<Comment> findByBlogPostIdAndParentIsNullOrderByCreatedAtDesc(Long blogPostId, Pageable pageable);

    Page<Comment> findByBookIdAndParentIsNullOrderByCreatedAtDesc(Long bookId, Pageable pageable);

    List<Comment> findByParentIdOrderByCreatedAtAsc(Long parentId);

    @Query("SELECT c FROM Comment c LEFT JOIN FETCH c.replies WHERE c.id = :id")
    java.util.Optional<Comment> findByIdWithReplies(@Param("id") Long id);

    Page<Comment> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Query("SELECT c FROM Comment c WHERE c.status = 'VISIBLE' ORDER BY c.createdAt DESC LIMIT :limit")
    List<Comment> findLatestComments(@Param("limit") int limit);

    Page<Comment> findByStatusOrderByCreatedAtDesc(CommentStatus status, Pageable pageable);

    @Query("SELECT c FROM Comment c ORDER BY c.createdAt DESC")
    Page<Comment> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
