package fit.hutech.BuiBaoHan.controllers.api;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fit.hutech.BuiBaoHan.dto.ApiResponse;
import fit.hutech.BuiBaoHan.entities.User;
import fit.hutech.BuiBaoHan.security.AuthResolver;
import fit.hutech.BuiBaoHan.services.LikeService;
import lombok.RequiredArgsConstructor;

/**
 * REST API Controller for Likes
 */
@RestController
@RequestMapping("/api/likes")
@RequiredArgsConstructor
public class ApiLikeController {

    private final LikeService likeService;
    private final AuthResolver authResolver;

    // ==================== Blog Post Likes ====================

    /**
     * Like a blog post
     */
    @PostMapping("/post/{postId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<LikeResult>> likePost(
            @AuthenticationPrincipal Object principal,
            @PathVariable Long postId) {
        User user = authResolver.resolveUser(principal);
        try {
            boolean liked = likeService.togglePostLike(user, postId);
            int count = likeService.getPostLikeCount(postId);
            return ResponseEntity.ok(ApiResponse.success(new LikeResult(liked, count)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Check if user liked a post
     */
    @GetMapping("/post/{postId}/check")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Boolean>> hasLikedPost(
            @AuthenticationPrincipal Object principal,
            @PathVariable Long postId) {
        User user = authResolver.resolveUser(principal);
        boolean liked = likeService.hasLikedPost(user, postId);
        return ResponseEntity.ok(ApiResponse.success(liked));
    }

    /**
     * Get post like count
     */
    @GetMapping("/post/{postId}/count")
    public ResponseEntity<ApiResponse<Integer>> getPostLikeCount(@PathVariable Long postId) {
        int count = likeService.getPostLikeCount(postId);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    // ==================== Comment Likes ====================

    /**
     * Like a comment
     */
    @PostMapping("/comment/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<LikeResult>> likeComment(
            @AuthenticationPrincipal Object principal,
            @PathVariable Long commentId) {
        User user = authResolver.resolveUser(principal);
        try {
            boolean liked = likeService.toggleCommentLike(user, commentId);
            int count = likeService.getCommentLikeCount(commentId);
            return ResponseEntity.ok(ApiResponse.success(new LikeResult(liked, count)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Check if user liked a comment
     */
    @GetMapping("/comment/{commentId}/check")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Boolean>> hasLikedComment(
            @AuthenticationPrincipal Object principal,
            @PathVariable Long commentId) {
        User user = authResolver.resolveUser(principal);
        boolean liked = likeService.hasLikedComment(user, commentId);
        return ResponseEntity.ok(ApiResponse.success(liked));
    }

    /**
     * Get comment like count
     */
    @GetMapping("/comment/{commentId}/count")
    public ResponseEntity<ApiResponse<Integer>> getCommentLikeCount(@PathVariable Long commentId) {
        int count = likeService.getCommentLikeCount(commentId);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    // ==================== Book Likes ====================

    /**
     * Like a book
     */
    @PostMapping("/book/{bookId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<LikeResult>> likeBook(
            @AuthenticationPrincipal Object principal,
            @PathVariable Long bookId) {
        User user = authResolver.resolveUser(principal);
        try {
            boolean liked = likeService.toggleBookLike(user, bookId);
            int count = likeService.getBookLikeCount(bookId);
            return ResponseEntity.ok(ApiResponse.success(new LikeResult(liked, count)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Check if user liked a book
     */
    @GetMapping("/book/{bookId}/check")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Boolean>> hasLikedBook(
            @AuthenticationPrincipal Object principal,
            @PathVariable Long bookId) {
        User user = authResolver.resolveUser(principal);
        boolean liked = likeService.hasLikedBook(user, bookId);
        return ResponseEntity.ok(ApiResponse.success(liked));
    }

    // ==================== Batch Check ====================

    /**
     * Batch check likes for multiple items
     */
    @GetMapping("/batch-check")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<LikeService.BatchLikeStatus>> batchCheck(
            @AuthenticationPrincipal Object principal,
            @RequestParam(required = false) Long[] postIds,
            @RequestParam(required = false) Long[] commentIds,
            @RequestParam(required = false) Long[] bookIds) {
        User user = authResolver.resolveUser(principal);
        var status = likeService.batchCheckLikes(user, postIds, commentIds, bookIds);
        return ResponseEntity.ok(ApiResponse.success(status));
    }

    // ==================== Inner Records ====================

    public record LikeResult(boolean liked, int count) {}
}
