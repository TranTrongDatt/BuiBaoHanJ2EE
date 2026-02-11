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
import fit.hutech.BuiBaoHan.dto.PageResponse;
import fit.hutech.BuiBaoHan.dto.PostCreateRequest;
import fit.hutech.BuiBaoHan.dto.PostUpdateRequest;
import fit.hutech.BuiBaoHan.entities.BlogPost;
import fit.hutech.BuiBaoHan.entities.User;
import fit.hutech.BuiBaoHan.security.AuthResolver;
import fit.hutech.BuiBaoHan.services.BlogPostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST API Controller for Blog management
 */
@RestController
@RequestMapping("/api/blog")
@RequiredArgsConstructor
public class ApiBlogController {

    private final BlogPostService blogPostService;
    private final AuthResolver authResolver;

    // ==================== Public Endpoints ====================

    /**
     * Get published posts (paginated)
     */
    @GetMapping("/posts")
    public ResponseEntity<ApiResponse<PageResponse<BlogPostSummary>>> getPosts(
            @PageableDefault(size = 12) Pageable pageable,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) Long bookId) {
        
        Page<BlogPost> posts;
        if (tag != null) {
            posts = blogPostService.findByTag(tag, pageable);
        } else if (bookId != null) {
            posts = blogPostService.findByBook(bookId, pageable);
        } else {
            posts = blogPostService.findPublished(pageable);
        }
        
        List<BlogPostSummary> dtos = posts.getContent().stream()
                .map(BlogPostSummary::from)
                .toList();
        
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(posts, dtos)));
    }

    /**
     * Get post by slug
     */
    @GetMapping("/posts/{slug}")
    public ResponseEntity<ApiResponse<BlogPostDetail>> getPostBySlug(@PathVariable String slug) {
        return blogPostService.findBySlug(slug)
                .map(post -> {
                    blogPostService.incrementViewCount(post.getId());
                    return ResponseEntity.ok(ApiResponse.success(BlogPostDetail.from(post)));
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.notFound("Post")));
    }

    /**
     * Search posts
     */
    @GetMapping("/posts/search")
    public ResponseEntity<ApiResponse<PageResponse<BlogPostSummary>>> searchPosts(
            @RequestParam String keyword,
            @PageableDefault(size = 12) Pageable pageable) {
        
        Page<BlogPost> posts = blogPostService.search(keyword, pageable);
        List<BlogPostSummary> dtos = posts.getContent().stream()
                .map(BlogPostSummary::from)
                .toList();
        
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(posts, dtos)));
    }

    /**
     * Get popular posts
     */
    @GetMapping("/posts/popular")
    public ResponseEntity<ApiResponse<List<BlogPostSummary>>> getPopularPosts(
            @RequestParam(defaultValue = "5") int limit) {
        List<BlogPostSummary> posts = blogPostService.findPopular(limit).stream()
                .map(BlogPostSummary::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(posts));
    }

    /**
     * Get recent posts
     */
    @GetMapping("/posts/recent")
    public ResponseEntity<ApiResponse<List<BlogPostSummary>>> getRecentPosts(
            @RequestParam(defaultValue = "5") int limit) {
        List<BlogPostSummary> posts = blogPostService.findRecent(limit).stream()
                .map(BlogPostSummary::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(posts));
    }

    /**
     * Get all tags
     */
    @GetMapping("/tags")
    public ResponseEntity<ApiResponse<List<TagSummary>>> getAllTags() {
        List<TagSummary> tags = blogPostService.getAllTags().stream()
                .map(tag -> new TagSummary(tag, 0))
                .toList();
        return ResponseEntity.ok(ApiResponse.success(tags));
    }

    // ==================== User Endpoints ====================

    /**
     * Get my posts
     */
    @GetMapping("/my-posts")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResponse<BlogPostSummary>>> getMyPosts(
            @AuthenticationPrincipal Object principal,
            @PageableDefault(size = 12) Pageable pageable,
            @RequestParam(required = false) String status) {
        User user = authResolver.resolveUser(principal);
        
        Page<BlogPost> posts = (status != null)
                ? blogPostService.findByUserAndStatus(user, status, pageable)
                : blogPostService.findByUser(user, pageable);
        
        List<BlogPostSummary> dtos = posts.getContent().stream()
                .map(BlogPostSummary::from)
                .toList();
        
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(posts, dtos)));
    }

    /**
     * Create new post
     */
    @PostMapping("/posts")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<BlogPostDetail>> createPost(
            @AuthenticationPrincipal Object principal,
            @Valid @RequestBody PostCreateRequest request) {
        User user = authResolver.resolveUser(principal);
        try {
            BlogPost post = blogPostService.create(user, request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.created(BlogPostDetail.from(post)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Update post
     */
    @PutMapping("/posts/{postId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<BlogPostDetail>> updatePost(
            @AuthenticationPrincipal Object principal,
            @PathVariable Long postId,
            @Valid @RequestBody PostUpdateRequest request) {
        User user = authResolver.resolveUser(principal);
        try {
            BlogPost post = blogPostService.update(postId, user, request);
            return ResponseEntity.ok(ApiResponse.updated(BlogPostDetail.from(post)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Delete post
     */
    @DeleteMapping("/posts/{postId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @AuthenticationPrincipal Object principal,
            @PathVariable Long postId) {
        User user = authResolver.resolveUser(principal);
        try {
            blogPostService.delete(postId, user);
            return ResponseEntity.ok(ApiResponse.deleted());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Publish post
     */
    @PostMapping("/posts/{postId}/publish")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<BlogPostDetail>> publishPost(
            @AuthenticationPrincipal Object principal,
            @PathVariable Long postId) {
        User user = authResolver.resolveUser(principal);
        try {
            BlogPost post = blogPostService.publish(postId, user);
            return ResponseEntity.ok(ApiResponse.success("Post published", BlogPostDetail.from(post)));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Unpublish post
     */
    @PostMapping("/posts/{postId}/unpublish")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<BlogPostDetail>> unpublishPost(
            @AuthenticationPrincipal Object principal,
            @PathVariable Long postId) {
        User user = authResolver.resolveUser(principal);
        try {
            BlogPost post = blogPostService.unpublish(postId, user);
            return ResponseEntity.ok(ApiResponse.success("Post unpublished", BlogPostDetail.from(post)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    // ==================== Admin Endpoints ====================

    /**
     * Get all posts (Admin)
     */
    @GetMapping("/admin/posts")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<BlogPostSummary>>> getAllPosts(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) String status) {
        
        Page<BlogPost> posts = (status != null)
                ? blogPostService.findByStatus(status, pageable)
                : blogPostService.findAll(pageable);
        
        List<BlogPostSummary> dtos = posts.getContent().stream()
                .map(BlogPostSummary::from)
                .toList();
        
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(posts, dtos)));
    }

    /**
     * Feature post (Admin)
     */
    @PostMapping("/admin/posts/{postId}/feature")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BlogPostDetail>> featurePost(@PathVariable Long postId) {
        try {
            BlogPost post = blogPostService.toggleFeatured(postId);
            return ResponseEntity.ok(ApiResponse.success(
                    post.getFeatured() ? "Post featured" : "Post unfeatured",
                    BlogPostDetail.from(post)
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    // ==================== Inner Records ====================

    public record BlogPostSummary(
            Long id,
            String title,
            String slug,
            String excerpt,
            String thumbnail,
            String authorName,
            String authorAvatar,
            int viewCount,
            int likeCount,
            int commentCount,
            List<String> tags,
            String createdAt,
            Boolean featured
    ) {
        public static BlogPostSummary from(BlogPost post) {
            return new BlogPostSummary(
                    post.getId(),
                    post.getTitle(),
                    post.getSlug(),
                    post.getExcerpt(),
                    post.getThumbnail(),
                    post.getAuthor().getFullName(),
                    post.getAuthor().getAvatar(),
                    post.getViewCount(),
                    post.getLikeCount(),
                    post.getCommentCount(),
                    post.getTags(),
                    post.getCreatedAt().toString(),
                    post.getFeatured()
            );
        }
    }

    public record BlogPostDetail(
            Long id,
            String title,
            String slug,
            String content,
            String excerpt,
            String thumbnail,
            AuthorInfo author,
            BookInfo book,
            int viewCount,
            int likeCount,
            int commentCount,
            List<String> tags,
            String status,
            Boolean featured,
            String createdAt,
            String publishedAt
    ) {
        public static BlogPostDetail from(BlogPost post) {
            return new BlogPostDetail(
                    post.getId(),
                    post.getTitle(),
                    post.getSlug(),
                    post.getContent(),
                    post.getExcerpt(),
                    post.getThumbnail(),
                    new AuthorInfo(
                            post.getAuthor().getId(),
                            post.getAuthor().getFullName(),
                            post.getAuthor().getAvatar(),
                            post.getAuthor().getUsername()
                    ),
                    post.getBook() != null ? new BookInfo(
                            post.getBook().getId(),
                            post.getBook().getTitle(),
                            post.getBook().getSlug(),
                            post.getBook().getCoverImage()
                    ) : null,
                    post.getViewCount(),
                    post.getLikeCount(),
                    post.getCommentCount(),
                    post.getTags(),
                    post.getStatus().name(),
                    post.getFeatured(),
                    post.getCreatedAt().toString(),
                    post.getPublishedAt() != null ? post.getPublishedAt().toString() : null
            );
        }
    }

    public record AuthorInfo(Long id, String fullName, String avatar, String username) {}
    public record BookInfo(Long id, String title, String slug, String coverImage) {}
    public record TagSummary(String name, int count) {}
}
