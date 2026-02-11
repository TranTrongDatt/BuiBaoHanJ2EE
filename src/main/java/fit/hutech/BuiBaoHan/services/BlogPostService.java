package fit.hutech.BuiBaoHan.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fit.hutech.BuiBaoHan.constants.PostStatus;
import fit.hutech.BuiBaoHan.constants.Visibility;
import fit.hutech.BuiBaoHan.dto.PostCreateRequest;
import fit.hutech.BuiBaoHan.dto.PostUpdateRequest;
import fit.hutech.BuiBaoHan.entities.BlogPost;
import fit.hutech.BuiBaoHan.entities.User;
import fit.hutech.BuiBaoHan.repositories.IBlogPostRepository;
import fit.hutech.BuiBaoHan.repositories.IUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service quản lý Blog Posts
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BlogPostService {

    private final IBlogPostRepository blogPostRepository;
    private final IUserRepository userRepository;
    private final SlugService slugService;

    /**
     * Lấy tất cả posts công khai
     */
    @Transactional(readOnly = true)
    public Page<BlogPost> getPublicPosts(Pageable pageable) {
        return blogPostRepository.findByStatusAndVisibility(
                PostStatus.PUBLISHED, Visibility.PUBLIC, pageable);
    }

    /**
     * Lấy posts của user
     */
    @Transactional(readOnly = true)
    public Page<BlogPost> getPostsByUser(Long userId, Pageable pageable) {
        return blogPostRepository.findByAuthorId(userId, pageable);
    }

    /**
     * Lấy posts công khai của user
     */
    @Transactional(readOnly = true)
    public Page<BlogPost> getPublicPostsByUser(Long userId, Pageable pageable) {
        return blogPostRepository.findByAuthorIdAndStatusAndVisibility(
                userId, PostStatus.PUBLISHED, Visibility.PUBLIC, pageable);
    }

    /**
     * Tìm post theo ID
     */
    @Transactional(readOnly = true)
    public Optional<BlogPost> getPostById(Long id) {
        return blogPostRepository.findById(id);
    }

    /**
     * Tìm post theo slug
     */
    @Transactional(readOnly = true)
    public Optional<BlogPost> getPostBySlug(String slug) {
        return blogPostRepository.findBySlug(slug);
    }

    /**
     * Tìm post với comments
     */
    @Transactional(readOnly = true)
    public Optional<BlogPost> getPostWithComments(Long id) {
        return blogPostRepository.findByIdWithComments(id);
    }

    /**
     * Tìm kiếm posts
     */
    @Transactional(readOnly = true)
    public Page<BlogPost> searchPosts(String keyword, Pageable pageable) {
        return blogPostRepository.searchByKeyword(keyword, pageable);
    }

    /**
     * Tạo post mới
     */
    public BlogPost createPost(Long authorId, PostCreateRequest request) {
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy user ID: " + authorId));

        // Generate slug
        String slug = slugService.toUniqueSlug(request.title(), blogPostRepository::existsBySlug);

        BlogPost post = BlogPost.builder()
                .author(author)
                .title(request.title())
                .slug(slug)
                .content(request.content())
                .summary(generateExcerpt(request.content()))
                .coverImage(request.coverImage())
                .visibility(request.visibility() != null ? request.visibility() : Visibility.PUBLIC)
                .mediaType(request.mediaType())
                .allowComments(Boolean.TRUE.equals(request.allowComments()) || request.allowComments() == null)
                .status(PostStatus.DRAFT)
                .viewCount(0L)
                .build();

        BlogPost saved = blogPostRepository.save(post);
        log.info("Created blog post: {} by user {}", saved.getSlug(), authorId);
        return saved;
    }

    /**
     * Cập nhật post
     */
    public BlogPost updatePost(Long postId, Long userId, PostUpdateRequest request) {
        BlogPost post = blogPostRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy post ID: " + postId));

        // Kiểm tra quyền
        if (!post.getAuthor().getId().equals(userId)) {
            throw new IllegalArgumentException("Không có quyền chỉnh sửa post này");
        }

        if (request.title() != null) {
            post.setTitle(request.title());
            // Regenerate slug nếu title thay đổi
            String newSlug = slugService.toUniqueSlug(request.title(), 
                    slug -> !slug.equals(post.getSlug()) && blogPostRepository.existsBySlug(slug));
            post.setSlug(newSlug);
        }
        if (request.content() != null) {
            post.setContent(request.content());
            post.setSummary(generateExcerpt(request.content()));
        }
        if (request.coverImage() != null) {
            post.setCoverImage(request.coverImage());
        }
        if (request.visibility() != null) {
            post.setVisibility(request.visibility());
        }
        if (request.allowComments() != null) {
            post.setAllowComments(request.allowComments());
        }

        post.setUpdatedAt(LocalDateTime.now());

        BlogPost updated = blogPostRepository.save(post);
        log.info("Updated blog post: {}", postId);
        return updated;
    }

    /**
     * Publish post
     */
    public BlogPost publishPost(Long postId, Long userId) {
        BlogPost post = blogPostRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy post ID: " + postId));

        // Kiểm tra quyền
        if (!post.getAuthor().getId().equals(userId)) {
            throw new IllegalArgumentException("Không có quyền publish post này");
        }

        post.setStatus(PostStatus.PUBLISHED);
        post.setPublishedAt(LocalDateTime.now());

        BlogPost published = blogPostRepository.save(post);
        log.info("Published blog post: {}", postId);
        return published;
    }

    /**
     * Unpublish post (chuyển về draft)
     */
    public BlogPost unpublishPost(Long postId, Long userId) {
        BlogPost post = blogPostRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy post ID: " + postId));

        if (!post.getAuthor().getId().equals(userId)) {
            throw new IllegalArgumentException("Không có quyền unpublish post này");
        }

        post.setStatus(PostStatus.DRAFT);
        post.setPublishedAt(null);

        BlogPost unpublished = blogPostRepository.save(post);
        log.info("Unpublished blog post: {}", postId);
        return unpublished;
    }

    /**
     * Archive post
     */
    public BlogPost archivePost(Long postId, Long userId) {
        BlogPost post = blogPostRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy post ID: " + postId));

        if (!post.getAuthor().getId().equals(userId)) {
            throw new IllegalArgumentException("Không có quyền archive post này");
        }

        post.setStatus(PostStatus.ARCHIVED);

        BlogPost archived = blogPostRepository.save(post);
        log.info("Archived blog post: {}", postId);
        return archived;
    }

    /**
     * Xóa post
     */
    public void deletePost(Long postId, Long userId) {
        BlogPost post = blogPostRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy post ID: " + postId));

        if (!post.getAuthor().getId().equals(userId)) {
            throw new IllegalArgumentException("Không có quyền xóa post này");
        }

        blogPostRepository.delete(post);
        log.info("Deleted blog post: {}", postId);
    }

    /**
     * Tăng view count
     */
    public void incrementViewCount(Long postId) {
        blogPostRepository.incrementViewCount(postId);
    }

    /**
     * Lấy posts trending (nhiều views nhất)
     */
    @Transactional(readOnly = true)
    public List<BlogPost> getTrendingPosts(int limit) {
        return blogPostRepository.findTrendingPosts(limit);
    }

    /**
     * Lấy posts mới nhất
     */
    @Transactional(readOnly = true)
    public List<BlogPost> getLatestPosts(int limit) {
        return blogPostRepository.findLatestPosts(limit);
    }

    /**
     * Kiểm tra user có thể xem post không
     */
    @Transactional(readOnly = true)
    public boolean canViewPost(Long postId, Long userId) {
        BlogPost post = blogPostRepository.findById(postId).orElse(null);
        if (post == null) return false;

        // Public post - ai cũng xem được
        if (post.getVisibility() == Visibility.PUBLIC && post.getStatus() == PostStatus.PUBLISHED) {
            return true;
        }

        // Author luôn xem được
        if (post.getAuthor().getId().equals(userId)) {
            return true;
        }

        // Followers only
        if (post.getVisibility() == Visibility.FOLLOWERS) {
            // TODO: Check follow relationship
            return false;
        }

        return false;
    }

    // ==================== Private Helper Methods ====================

    private String generateExcerpt(String content) {
        if (content == null || content.length() <= 200) {
            return content;
        }
        // Lấy 200 ký tự đầu + "..."
        return content.substring(0, 200).trim() + "...";
    }

    // ==================== Wrapper Methods for Controller Compatibility ====================

    /**
     * Tìm post theo slug (wrapper)
     */
    @Transactional(readOnly = true)
    public java.util.Optional<BlogPost> findBySlug(String slug) {
        return getPostBySlug(slug);
    }

    /**
     * Tìm các post đã published (wrapper)
     */
    @Transactional(readOnly = true)
    public Page<BlogPost> findPublished(Pageable pageable) {
        return getPublicPosts(pageable);
    }

    /**
     * Tìm kiếm posts (wrapper)
     */
    @Transactional(readOnly = true)
    public Page<BlogPost> search(String keyword, Pageable pageable) {
        return searchPosts(keyword, pageable);
    }

    /**
     * Tìm posts phổ biến nhất (theo views)
     */
    @Transactional(readOnly = true)
    public java.util.List<BlogPost> findPopular(int limit) {
        return getTrendingPosts(limit);
    }

    /**
     * Tìm posts nổi bật (featured/pinned)
     */
    @Transactional(readOnly = true)
    public java.util.List<BlogPost> findFeatured(int limit) {
        return blogPostRepository.findByIsPinnedTrue(PageRequest.of(0, limit)).getContent();
    }

    /**
     * Tìm posts theo tag (placeholder - cần thêm bảng tags sau)
     */
    @Transactional(readOnly = true)
    public Page<BlogPost> findByTag(String tag, Pageable pageable) {
        // TODO: Implement khi có bảng tags
        return searchPosts(tag, pageable);
    }

    /**
     * Lấy tất cả tags (placeholder)
     */
    @Transactional(readOnly = true)
    public java.util.List<String> getAllTags() {
        // TODO: Implement khi có bảng tags
        return java.util.List.of();
    }

    /**
     * Tìm posts liên quan
     */
    @Transactional(readOnly = true)
    public java.util.List<BlogPost> findRelated(BlogPost post, int limit) {
        if (post.getBook() != null) {
            return blogPostRepository.findByBookIdAndIdNot(post.getBook().getId(), post.getId(), 
                    PageRequest.of(0, limit)).getContent();
        }
        return getLatestPosts(limit);
    }

    /**
     * Tìm posts của user theo status
     */
    @Transactional(readOnly = true)
    public Page<BlogPost> findByUserAndStatus(User user, String status, Pageable pageable) {
        PostStatus postStatus = PostStatus.valueOf(status.toUpperCase());
        return blogPostRepository.findByAuthorIdAndStatus(user.getId(), postStatus, pageable);
    }

    /**
     * Tìm tất cả posts của user
     */
    @Transactional(readOnly = true)
    public Page<BlogPost> findByUser(User user, Pageable pageable) {
        return getPostsByUser(user.getId(), pageable);
    }

    /**
     * Tìm posts theo username của author
     */
    @Transactional(readOnly = true)
    public Page<BlogPost> findByAuthorUsername(String username, Pageable pageable) {
        return blogPostRepository.findByAuthorUsernameAndStatusAndVisibility(
                username, PostStatus.PUBLISHED, Visibility.PUBLIC, pageable);
    }

    /**
     * Tạo post (wrapper lấy User object)
     */
    public BlogPost create(User user, PostCreateRequest request) {
        return createPost(user.getId(), request);
    }

    /**
     * Cập nhật post (wrapper lấy User object)
     */
    public BlogPost update(Long postId, User user, PostUpdateRequest request) {
        return updatePost(postId, user.getId(), request);
    }

    /**
     * Xóa post (wrapper lấy User object)
     */
    public void delete(Long postId, User user) {
        deletePost(postId, user.getId());
    }

    /**
     * Tìm posts theo book ID
     */
    @Transactional(readOnly = true)
    public Page<BlogPost> findByBook(Long bookId, Pageable pageable) {
        return blogPostRepository.findByBookIdAndIdNot(bookId, -1L, pageable);
    }

    /**
     * Tìm posts mới nhất (wrapper cho getLatestPosts)
     */
    @Transactional(readOnly = true)
    public List<BlogPost> findRecent(int limit) {
        return getLatestPosts(limit);
    }

    /**
     * Publish post (wrapper lấy User object)
     */
    public BlogPost publish(Long postId, User user) {
        return publishPost(postId, user.getId());
    }

    /**
     * Unpublish post (wrapper lấy User object)
     */
    public BlogPost unpublish(Long postId, User user) {
        return unpublishPost(postId, user.getId());
    }

    /**
     * Tìm posts theo status (Admin)
     */
    @Transactional(readOnly = true)
    public Page<BlogPost> findByStatus(String status, Pageable pageable) {
        PostStatus postStatus = PostStatus.valueOf(status.toUpperCase());
        return blogPostRepository.findByStatus(postStatus, pageable);
    }

    /**
     * Lấy tất cả posts (Admin)
     */
    @Transactional(readOnly = true)
    public Page<BlogPost> findAll(Pageable pageable) {
        return blogPostRepository.findAll(pageable);
    }

    /**
     * Toggle featured status của post (Admin)
     */
    public BlogPost toggleFeatured(Long postId) {
        BlogPost post = blogPostRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy post ID: " + postId));
        post.setIsPinned(!Boolean.TRUE.equals(post.getIsPinned()));
        BlogPost updated = blogPostRepository.save(post);
        log.info("Toggled featured status for post: {} to {}", postId, post.getIsPinned());
        return updated;
    }
}
