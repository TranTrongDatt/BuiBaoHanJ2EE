package fit.hutech.BuiBaoHan.controllers.api;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fit.hutech.BuiBaoHan.dto.ApiResponse;
import fit.hutech.BuiBaoHan.dto.CommentRequest;
import fit.hutech.BuiBaoHan.dto.PageResponse;
import fit.hutech.BuiBaoHan.entities.Comment;
import fit.hutech.BuiBaoHan.entities.User;
import fit.hutech.BuiBaoHan.security.AuthResolver;
import fit.hutech.BuiBaoHan.services.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST API Controller for Comments
 */
@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class ApiCommentController {

    private final CommentService commentService;
    private final AuthResolver authResolver;

    // ==================== Public Endpoints ====================

    /**
     * Get comments for a post
     */
    @GetMapping("/post/{postId}")
    public ResponseEntity<ApiResponse<PageResponse<CommentResponse>>> getPostComments(
            @PathVariable Long postId,
            @PageableDefault(size = 20) Pageable pageable) {
        
        Page<Comment> comments = commentService.findByPost(postId, pageable);
        List<CommentResponse> dtos = comments.getContent().stream()
                .map(CommentResponse::from)
                .toList();
        
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(comments, dtos)));
    }

    /**
     * Get comments for a book (reviews)
     */
    @GetMapping("/book/{bookId}")
    public ResponseEntity<ApiResponse<PageResponse<CommentResponse>>> getBookComments(
            @PathVariable Long bookId,
            @PageableDefault(size = 20) Pageable pageable) {
        
        Page<Comment> comments = commentService.findByBook(bookId, pageable);
        List<CommentResponse> dtos = comments.getContent().stream()
                .map(CommentResponse::from)
                .toList();
        
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(comments, dtos)));
    }

    /**
     * Get replies to a comment
     */
    @GetMapping("/{commentId}/replies")
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getReplies(@PathVariable Long commentId) {
        List<CommentResponse> replies = commentService.findReplies(commentId).stream()
                .map(CommentResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(replies));
    }

    // ==================== User Endpoints ====================

    /**
     * Add comment to post
     */
    @PostMapping("/post/{postId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CommentResponse>> addPostComment(
            @AuthenticationPrincipal Object principal,
            @PathVariable Long postId,
            @Valid @RequestBody CommentRequest request) {
        User user = authResolver.resolveUser(principal);
        try {
            Comment comment = commentService.addPostComment(user, postId, request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.created(CommentResponse.from(comment)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Add comment/review to book
     */
    @PostMapping("/book/{bookId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CommentResponse>> addBookComment(
            @AuthenticationPrincipal Object principal,
            @PathVariable Long bookId,
            @Valid @RequestBody CommentRequest request) {
        User user = authResolver.resolveUser(principal);
        try {
            Comment comment = commentService.addBookComment(user, bookId, request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.created(CommentResponse.from(comment)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Reply to a comment
     */
    @PostMapping("/{commentId}/reply")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CommentResponse>> replyToComment(
            @AuthenticationPrincipal Object principal,
            @PathVariable Long commentId,
            @Valid @RequestBody CommentRequest request) {
        User user = authResolver.resolveUser(principal);
        try {
            Comment reply = commentService.replyToComment(user, commentId, request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.created(CommentResponse.from(reply)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Update comment
     */
    @PutMapping("/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CommentResponse>> updateComment(
            @AuthenticationPrincipal Object principal,
            @PathVariable Long commentId,
            @Valid @RequestBody CommentRequest request) {
        User user = authResolver.resolveUser(principal);
        try {
            Comment comment = commentService.update(commentId, user, request);
            return ResponseEntity.ok(ApiResponse.updated(CommentResponse.from(comment)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Delete comment
     */
    @DeleteMapping("/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @AuthenticationPrincipal Object principal,
            @PathVariable Long commentId) {
        User user = authResolver.resolveUser(principal);
        try {
            commentService.delete(commentId, user);
            return ResponseEntity.ok(ApiResponse.deleted());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    // ==================== Admin Endpoints ====================

    /**
     * Get all comments (Admin)
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<CommentResponse>>> getAllComments(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) String status) {
        
        Page<Comment> comments = (status != null)
                ? commentService.findByStatus(status, pageable)
                : commentService.findAll(pageable);
        
        List<CommentResponse> dtos = comments.getContent().stream()
                .map(CommentResponse::from)
                .toList();
        
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(comments, dtos)));
    }

    /**
     * Approve comment (Admin)
     */
    @PostMapping("/{commentId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CommentResponse>> approveComment(@PathVariable Long commentId) {
        try {
            Comment comment = commentService.approve(commentId);
            return ResponseEntity.ok(ApiResponse.success("Comment approved", CommentResponse.from(comment)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Reject comment (Admin)
     */
    @PostMapping("/{commentId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> rejectComment(
            @PathVariable Long commentId,
            @RequestParam(required = false) String reason) {
        try {
            commentService.reject(commentId, reason);
            return ResponseEntity.ok(ApiResponse.success("Comment rejected"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Admin delete comment
     */
    @DeleteMapping("/admin/{commentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> adminDeleteComment(@PathVariable Long commentId) {
        try {
            commentService.adminDelete(commentId);
            return ResponseEntity.ok(ApiResponse.deleted());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    // ==================== Inner Records ====================

    public record CommentResponse(
            Long id,
            String content,
            Integer rating,
            AuthorInfo author,
            int likeCount,
            int replyCount,
            Long parentId,
            String status,
            String createdAt,
            String updatedAt
    ) {
        public static CommentResponse from(Comment comment) {
            return new CommentResponse(
                    comment.getId(),
                    comment.getContent(),
                    comment.getRating(),
                    new AuthorInfo(
                            comment.getUser().getId(),
                            comment.getUser().getFullName(),
                            comment.getUser().getAvatar(),
                            comment.getUser().getUsername()
                    ),
                    comment.getLikeCount() != null ? comment.getLikeCount().intValue() : 0,
                    comment.getReplyCount(),
                    comment.getParent() != null ? comment.getParent().getId() : null,
                    comment.getStatus().name(),
                    comment.getCreatedAt().toString(),
                    comment.getUpdatedAt() != null ? comment.getUpdatedAt().toString() : null
            );
        }
    }

    public record AuthorInfo(Long id, String fullName, String avatar, String username) {}
}
