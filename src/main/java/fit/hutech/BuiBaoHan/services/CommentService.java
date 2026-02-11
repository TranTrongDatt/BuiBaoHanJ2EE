package fit.hutech.BuiBaoHan.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fit.hutech.BuiBaoHan.dto.CommentRequest;
import fit.hutech.BuiBaoHan.entities.BlogPost;
import fit.hutech.BuiBaoHan.entities.Book;
import fit.hutech.BuiBaoHan.entities.Comment;
import fit.hutech.BuiBaoHan.entities.User;
import fit.hutech.BuiBaoHan.repositories.IBlogPostRepository;
import fit.hutech.BuiBaoHan.repositories.IBookRepository;
import fit.hutech.BuiBaoHan.repositories.ICommentRepository;
import fit.hutech.BuiBaoHan.repositories.IUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service quản lý Comments
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CommentService {

    private final ICommentRepository commentRepository;
    private final IUserRepository userRepository;
    private final IBlogPostRepository blogPostRepository;
    private final IBookRepository bookRepository;
    private final NotificationService notificationService;

    // ==================== API Wrapper Methods ====================

    /**
     * Lấy comments của blog post (wrapper for API)
     */
    @Transactional(readOnly = true)
    public Page<Comment> findByPost(Long postId, Pageable pageable) {
        return getCommentsByBlogPost(postId, pageable);
    }

    /**
     * Lấy comments của sách (wrapper for API)
     */
    @Transactional(readOnly = true)
    public Page<Comment> findByBook(Long bookId, Pageable pageable) {
        return getCommentsByBook(bookId, pageable);
    }

    /**
     * Lấy replies của comment (wrapper for API)
     */
    @Transactional(readOnly = true)
    public List<Comment> findReplies(Long commentId) {
        return getReplies(commentId);
    }

    /**
     * Thêm comment cho blog post (wrapper for API - User object)
     */
    public Comment addPostComment(User user, Long postId, CommentRequest request) {
        CommentRequest updatedRequest = new CommentRequest(
                request.content(), postId, null, null, request.rating());
        return createComment(user.getId(), updatedRequest);
    }

    /**
     * Thêm comment cho sách (wrapper for API - User object)
     */
    public Comment addBookComment(User user, Long bookId, CommentRequest request) {
        CommentRequest updatedRequest = new CommentRequest(
                request.content(), null, bookId, null, request.rating());
        return createComment(user.getId(), updatedRequest);
    }

    /**
     * Trả lời comment (wrapper for API - User object)
     */
    public Comment replyToComment(User user, Long commentId, CommentRequest request) {
        Comment parent = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy comment ID: " + commentId));
        
        CommentRequest updatedRequest = new CommentRequest(
                request.content(),
                parent.getBlogPost() != null ? parent.getBlogPost().getId() : null,
                parent.getBook() != null ? parent.getBook().getId() : null,
                commentId,
                request.rating());
        return createComment(user.getId(), updatedRequest);
    }

    /**
     * Cập nhật comment (wrapper for API - User object)
     */
    public Comment update(Long commentId, User user, CommentRequest request) {
        return updateComment(commentId, user.getId(), request.content());
    }

    /**
     * Xóa comment (wrapper for API - User object)
     */
    public void delete(Long commentId, User user) {
        deleteComment(commentId, user.getId());
    }

    /**
     * Tìm comments theo trạng thái (Admin)
     */
    @Transactional(readOnly = true)
    public Page<Comment> findByStatus(String status, Pageable pageable) {
        try {
            fit.hutech.BuiBaoHan.constants.CommentStatus commentStatus = 
                    fit.hutech.BuiBaoHan.constants.CommentStatus.valueOf(status.toUpperCase());
            return commentRepository.findByStatusOrderByCreatedAtDesc(commentStatus, pageable);
        } catch (IllegalArgumentException e) {
            log.warn("Unknown comment status: {}", status);
            return Page.empty(pageable);
        }
    }

    /**
     * Lấy tất cả comments (Admin)
     */
    @Transactional(readOnly = true)
    public Page<Comment> findAll(Pageable pageable) {
        return commentRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    /**
     * Duyệt comment (Admin)
     */
    public Comment approve(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy comment ID: " + commentId));
        
        comment.setStatus(fit.hutech.BuiBaoHan.constants.CommentStatus.VISIBLE);
        Comment approved = commentRepository.save(comment);
        log.info("Approved comment {}", commentId);
        return approved;
    }

    /**
     * Từ chối comment (Admin)
     */
    public void reject(Long commentId, String reason) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy comment ID: " + commentId));
        
        comment.setStatus(fit.hutech.BuiBaoHan.constants.CommentStatus.REJECTED);
        commentRepository.save(comment);
        log.info("Rejected comment {} with reason: {}", commentId, reason);
    }

    /**
     * Xóa comment (Admin - không kiểm tra quyền)
     */
    public void adminDelete(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy comment ID: " + commentId));
        
        // Soft delete nếu có replies
        if (!comment.getReplies().isEmpty()) {
            comment.setIsDeleted(true);
            comment.setContent("[Comment đã bị xóa bởi Admin]");
            comment.setStatus(fit.hutech.BuiBaoHan.constants.CommentStatus.DELETED);
            commentRepository.save(comment);
        } else {
            commentRepository.delete(comment);
        }
        log.info("Admin deleted comment {}", commentId);
    }

    // ==================== Core Methods ====================

    /**
     * Lấy comments của blog post
     */
    @Transactional(readOnly = true)
    public Page<Comment> getCommentsByBlogPost(Long blogPostId, Pageable pageable) {
        return commentRepository.findByBlogPostIdAndParentIsNullOrderByCreatedAtDesc(blogPostId, pageable);
    }

    /**
     * Lấy comments của sách
     */
    @Transactional(readOnly = true)
    public Page<Comment> getCommentsByBook(Long bookId, Pageable pageable) {
        return commentRepository.findByBookIdAndParentIsNullOrderByCreatedAtDesc(bookId, pageable);
    }

    /**
     * Lấy replies của comment
     */
    @Transactional(readOnly = true)
    public List<Comment> getReplies(Long parentId) {
        return commentRepository.findByParentIdOrderByCreatedAtAsc(parentId);
    }

    /**
     * Tìm comment theo ID
     */
    @Transactional(readOnly = true)
    public Optional<Comment> getCommentById(Long id) {
        return commentRepository.findById(id);
    }

    /**
     * Tìm comment với replies
     */
    @Transactional(readOnly = true)
    public Optional<Comment> getCommentWithReplies(Long id) {
        return commentRepository.findByIdWithReplies(id);
    }

    /**
     * Lấy comments của user
     */
    @Transactional(readOnly = true)
    public Page<Comment> getCommentsByUser(Long userId, Pageable pageable) {
        return commentRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    /**
     * Tạo comment mới
     */
    public Comment createComment(Long userId, CommentRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy user ID: " + userId));

        Comment.CommentBuilder builder = Comment.builder()
                .user(user)
                .content(request.content())
                .rating(request.rating())
                .createdAt(LocalDateTime.now());

        // Comment cho blog post
        if (request.blogPostId() != null) {
            BlogPost blogPost = blogPostRepository.findById(request.blogPostId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy blog post ID: " + request.blogPostId()));

            if (!blogPost.getAllowComments()) {
                throw new IllegalStateException("Bài viết không cho phép comment");
            }

            builder.blogPost(blogPost);

            // Gửi thông báo cho author
            if (!blogPost.getAuthor().getId().equals(userId)) {
                notificationService.sendCommentNotification(
                        blogPost.getAuthor().getId(), user, blogPost, request.content());
            }
        }

        // Comment cho sách
        if (request.bookId() != null) {
            Book book = bookRepository.findById(request.bookId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sách ID: " + request.bookId()));
            builder.book(book);
        }

        // Reply cho comment khác
        if (request.parentCommentId() != null) {
            Comment parent = commentRepository.findById(request.parentCommentId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy comment ID: " + request.parentCommentId()));
            builder.parent(parent);

            // Gửi thông báo cho người được reply
            if (!parent.getUser().getId().equals(userId)) {
                notificationService.sendReplyNotification(
                        parent.getUser().getId(), user, parent, request.content());
            }
        }

        Comment comment = builder.build();
        Comment saved = commentRepository.save(comment);
        log.info("Created comment {} by user {}", saved.getId(), userId);
        return saved;
    }

    /**
     * Cập nhật comment
     */
    public Comment updateComment(Long commentId, Long userId, String newContent) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy comment ID: " + commentId));

        // Kiểm tra quyền
        if (!comment.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Không có quyền chỉnh sửa comment này");
        }

        comment.setContent(newContent);
        comment.setUpdatedAt(LocalDateTime.now());
        comment.setIsEdited(true);

        Comment updated = commentRepository.save(comment);
        log.info("Updated comment {}", commentId);
        return updated;
    }

    /**
     * Xóa comment
     */
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy comment ID: " + commentId));

        // Kiểm tra quyền (chủ comment hoặc admin)
        if (!comment.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Không có quyền xóa comment này");
        }

        // Soft delete nếu có replies
        if (!comment.getReplies().isEmpty()) {
            comment.setIsDeleted(true);
            comment.setContent("[Comment đã bị xóa]");
            commentRepository.save(comment);
        } else {
            commentRepository.delete(comment);
        }

        log.info("Deleted comment {}", commentId);
    }

    /**
     * Đếm comments của blog post
     */
    @Transactional(readOnly = true)
    public long countByBlogPost(Long blogPostId) {
        return commentRepository.countByBlogPostId(blogPostId);
    }

    /**
     * Đếm comments của sách
     */
    @Transactional(readOnly = true)
    public long countByBook(Long bookId) {
        return commentRepository.countByBookId(bookId);
    }

    /**
     * Lấy comments mới nhất
     */
    @Transactional(readOnly = true)
    public List<Comment> getLatestComments(int limit) {
        return commentRepository.findLatestComments(limit);
    }

    /**
     * Ẩn comment (Admin)
     */
    public Comment hideComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy comment ID: " + commentId));

        comment.setIsHidden(true);
        Comment hidden = commentRepository.save(comment);
        log.info("Hidden comment {}", commentId);
        return hidden;
    }

    /**
     * Hiện comment (Admin)
     */
    public Comment unhideComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy comment ID: " + commentId));

        comment.setIsHidden(false);
        Comment shown = commentRepository.save(comment);
        log.info("Unhidden comment {}", commentId);
        return shown;
    }
}
