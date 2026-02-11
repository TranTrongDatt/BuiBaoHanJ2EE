package fit.hutech.BuiBaoHan.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fit.hutech.BuiBaoHan.constants.LikeType;
import fit.hutech.BuiBaoHan.entities.BlogPost;
import fit.hutech.BuiBaoHan.entities.Book;
import fit.hutech.BuiBaoHan.entities.Comment;
import fit.hutech.BuiBaoHan.entities.Like;
import fit.hutech.BuiBaoHan.entities.User;
import fit.hutech.BuiBaoHan.repositories.IBlogPostRepository;
import fit.hutech.BuiBaoHan.repositories.IBookRepository;
import fit.hutech.BuiBaoHan.repositories.ICommentRepository;
import fit.hutech.BuiBaoHan.repositories.ILikeRepository;
import fit.hutech.BuiBaoHan.repositories.IUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service quản lý Likes
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LikeService {

    private final ILikeRepository likeRepository;
    private final IUserRepository userRepository;
    private final IBlogPostRepository blogPostRepository;
    private final IBookRepository bookRepository;
    private final ICommentRepository commentRepository;
    private final NotificationService notificationService;

    // ==================== Blog Post Likes ====================

    /**
     * Like blog post
     */
    public Like likeBlogPost(Long userId, Long blogPostId) {
        if (isLikedBlogPost(userId, blogPostId)) {
            throw new IllegalStateException("Bạn đã like bài viết này");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy user ID: " + userId));
        BlogPost blogPost = blogPostRepository.findById(blogPostId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy blog post ID: " + blogPostId));

        Like like = Like.builder()
                .user(user)
                .blogPost(blogPost)
                .likeType(LikeType.LIKE)
                .build();

        Like saved = likeRepository.save(like);

        // Gửi thông báo cho author
        if (!blogPost.getAuthor().getId().equals(userId)) {
            notificationService.sendLikeNotification(blogPost.getAuthor().getId(), user, blogPost);
        }

        log.info("User {} liked blog post {}", userId, blogPostId);
        return saved;
    }

    /**
     * Unlike blog post
     */
    public void unlikeBlogPost(Long userId, Long blogPostId) {
        Optional<Like> like = likeRepository.findByUserIdAndBlogPostId(userId, blogPostId);
        if (like.isEmpty()) {
            throw new IllegalArgumentException("Bạn chưa like bài viết này");
        }

        likeRepository.delete(like.get());
        log.info("User {} unliked blog post {}", userId, blogPostId);
    }

    /**
     * Toggle like blog post
     */
    public boolean toggleLikeBlogPost(Long userId, Long blogPostId) {
        if (isLikedBlogPost(userId, blogPostId)) {
            unlikeBlogPost(userId, blogPostId);
            return false;
        } else {
            likeBlogPost(userId, blogPostId);
            return true;
        }
    }

    /**
     * Kiểm tra đã like blog post chưa
     */
    @Transactional(readOnly = true)
    public boolean isLikedBlogPost(Long userId, Long blogPostId) {
        return likeRepository.existsByUserIdAndBlogPostId(userId, blogPostId);
    }

    /**
     * Đếm likes của blog post
     */
    @Transactional(readOnly = true)
    public long countLikesByBlogPost(Long blogPostId) {
        return likeRepository.countByBlogPostId(blogPostId);
    }

    /**
     * Lấy danh sách user đã like blog post
     */
    @Transactional(readOnly = true)
    public List<User> getUsersLikedBlogPost(Long blogPostId) {
        return likeRepository.findUsersByBlogPostId(blogPostId);
    }

    // ==================== Book Likes ====================

    /**
     * Like sách
     */
    public Like likeBook(Long userId, Long bookId) {
        if (isLikedBook(userId, bookId)) {
            throw new IllegalStateException("Bạn đã like sách này");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy user ID: " + userId));
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sách ID: " + bookId));

        Like like = Like.builder()
                .user(user)
                .book(book)
                .likeType(LikeType.LIKE)
                .build();

        Like saved = likeRepository.save(like);
        log.info("User {} liked book {}", userId, bookId);
        return saved;
    }

    /**
     * Unlike sách
     */
    public void unlikeBook(Long userId, Long bookId) {
        Optional<Like> like = likeRepository.findByUserIdAndBookId(userId, bookId);
        if (like.isEmpty()) {
            throw new IllegalArgumentException("Bạn chưa like sách này");
        }

        likeRepository.delete(like.get());
        log.info("User {} unliked book {}", userId, bookId);
    }

    /**
     * Toggle like sách
     */
    public boolean toggleLikeBook(Long userId, Long bookId) {
        if (isLikedBook(userId, bookId)) {
            unlikeBook(userId, bookId);
            return false;
        } else {
            likeBook(userId, bookId);
            return true;
        }
    }

    /**
     * Kiểm tra đã like sách chưa
     */
    @Transactional(readOnly = true)
    public boolean isLikedBook(Long userId, Long bookId) {
        return likeRepository.existsByUserIdAndBookId(userId, bookId);
    }

    /**
     * Đếm likes của sách
     */
    @Transactional(readOnly = true)
    public long countLikesByBook(Long bookId) {
        return likeRepository.countByBookId(bookId);
    }

    // ==================== Comment Likes ====================

    /**
     * Like comment
     */
    public Like likeComment(Long userId, Long commentId) {
        if (isLikedComment(userId, commentId)) {
            throw new IllegalStateException("Bạn đã like comment này");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy user ID: " + userId));
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy comment ID: " + commentId));

        Like like = Like.builder()
                .user(user)
                .comment(comment)
                .likeType(LikeType.LIKE)
                .build();

        Like saved = likeRepository.save(like);
        log.info("User {} liked comment {}", userId, commentId);
        return saved;
    }

    /**
     * Unlike comment
     */
    public void unlikeComment(Long userId, Long commentId) {
        Optional<Like> like = likeRepository.findByUserIdAndCommentId(userId, commentId);
        if (like.isEmpty()) {
            throw new IllegalArgumentException("Bạn chưa like comment này");
        }

        likeRepository.delete(like.get());
        log.info("User {} unliked comment {}", userId, commentId);
    }

    /**
     * Toggle like comment
     */
    public boolean toggleLikeComment(Long userId, Long commentId) {
        if (isLikedComment(userId, commentId)) {
            unlikeComment(userId, commentId);
            return false;
        } else {
            likeComment(userId, commentId);
            return true;
        }
    }

    /**
     * Kiểm tra đã like comment chưa
     */
    @Transactional(readOnly = true)
    public boolean isLikedComment(Long userId, Long commentId) {
        return likeRepository.existsByUserIdAndCommentId(userId, commentId);
    }

    /**
     * Đếm likes của comment
     */
    @Transactional(readOnly = true)
    public long countLikesByComment(Long commentId) {
        return likeRepository.countByCommentId(commentId);
    }

    // ==================== Statistics ====================

    /**
     * Sách được like nhiều nhất
     */
    @Transactional(readOnly = true)
    public List<Book> getMostLikedBooks(int limit) {
        return likeRepository.findMostLikedBooks(limit);
    }

    /**
     * Blog posts được like nhiều nhất
     */
    @Transactional(readOnly = true)
    public List<BlogPost> getMostLikedBlogPosts(int limit) {
        return likeRepository.findMostLikedBlogPosts(limit);
    }

    // ==================== User-based methods (wrapper) ====================

    /**
     * Toggle like post (with User object)
     */
    public boolean togglePostLike(User user, Long postId) {
        return toggleLikeBlogPost(user.getId(), postId);
    }

    /**
     * Get post like count
     */
    @Transactional(readOnly = true)
    public int getPostLikeCount(Long postId) {
        return (int) countLikesByBlogPost(postId);
    }

    /**
     * Check if user has liked post
     */
    @Transactional(readOnly = true)
    public boolean hasLikedPost(User user, Long postId) {
        return isLikedBlogPost(user.getId(), postId);
    }

    /**
     * Toggle like comment (with User object)
     */
    public boolean toggleCommentLike(User user, Long commentId) {
        return toggleLikeComment(user.getId(), commentId);
    }

    /**
     * Get comment like count
     */
    @Transactional(readOnly = true)
    public int getCommentLikeCount(Long commentId) {
        return (int) countLikesByComment(commentId);
    }

    /**
     * Check if user has liked comment
     */
    @Transactional(readOnly = true)
    public boolean hasLikedComment(User user, Long commentId) {
        return isLikedComment(user.getId(), commentId);
    }

    /**
     * Toggle like book (with User object)
     */
    public boolean toggleBookLike(User user, Long bookId) {
        return toggleLikeBook(user.getId(), bookId);
    }

    /**
     * Get book like count
     */
    @Transactional(readOnly = true)
    public int getBookLikeCount(Long bookId) {
        return (int) countLikesByBook(bookId);
    }

    /**
     * Check if user has liked book
     */
    @Transactional(readOnly = true)
    public boolean hasLikedBook(User user, Long bookId) {
        return isLikedBook(user.getId(), bookId);
    }

    /**
     * Batch check likes for multiple items
     */
    @Transactional(readOnly = true)
    public BatchLikeStatus batchCheckLikes(User user, Long[] postIds, Long[] commentIds, Long[] bookIds) {
        Map<Long, Boolean> postLikes = new HashMap<>();
        if (postIds != null) {
            for (Long postId : postIds) {
                postLikes.put(postId, isLikedBlogPost(user.getId(), postId));
            }
        }

        Map<Long, Boolean> commentLikes = new HashMap<>();
        if (commentIds != null) {
            for (Long commentId : commentIds) {
                commentLikes.put(commentId, isLikedComment(user.getId(), commentId));
            }
        }

        Map<Long, Boolean> bookLikes = new HashMap<>();
        if (bookIds != null) {
            for (Long bookId : bookIds) {
                bookLikes.put(bookId, isLikedBook(user.getId(), bookId));
            }
        }

        return new BatchLikeStatus(postLikes, commentLikes, bookLikes);
    }

    /**
     * Batch like status record
     */
    public record BatchLikeStatus(
            Map<Long, Boolean> postLikes,
            Map<Long, Boolean> commentLikes,
            Map<Long, Boolean> bookLikes
    ) {}
}
