package fit.hutech.BuiBaoHan.entities;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import fit.hutech.BuiBaoHan.constants.MediaType;
import fit.hutech.BuiBaoHan.constants.PostStatus;
import fit.hutech.BuiBaoHan.constants.Visibility;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity đại diện cho Bài viết Blog
 */
@Entity
@Table(name = "blog_post")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlogPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Tiêu đề không được để trống")
    @Column(name = "title", length = 255, nullable = false)
    private String title;

    @Column(name = "slug", length = 300, unique = true)
    private String slug;

    @Column(name = "summary", length = 500)
    private String summary;

    @NotBlank(message = "Nội dung không được để trống")
    @Column(name = "content", columnDefinition = "LONGTEXT", nullable = false)
    private String content;

    @Column(name = "cover_image", length = 255)
    private String coverImage;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", length = 20)
    private MediaType mediaType = MediaType.TEXT;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", length = 20)
    private Visibility visibility = Visibility.PUBLIC;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private PostStatus status = PostStatus.ACTIVE;

    @Builder.Default
    @Column(name = "view_count")
    private Long viewCount = 0L;

    @Builder.Default
    @Column(name = "like_count")
    private Long likeCount = 0L;

    @Builder.Default
    @Column(name = "comment_count")
    private Long commentCount = 0L;

    @Builder.Default
    @Column(name = "share_count")
    private Long shareCount = 0L;

    @Builder.Default
    @Column(name = "is_pinned")
    private Boolean isPinned = false;

    @Builder.Default
    @Column(name = "allow_comments")
    private Boolean allowComments = true;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Quan hệ với User (tác giả bài viết)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    // Quan hệ với Book (bài viết liên quan đến sách)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id")
    private Book book;

    // Danh sách comment
    @Builder.Default
    @OneToMany(mappedBy = "blogPost", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();

    // Danh sách like
    @Builder.Default
    @OneToMany(mappedBy = "blogPost", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Like> likes = new ArrayList<>();

    // Helper methods
    public void incrementViewCount() {
        this.viewCount++;
    }

    public void incrementLikeCount() {
        this.likeCount++;
    }

    public void decrementLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }

    public void incrementCommentCount() {
        this.commentCount++;
    }

    public void decrementCommentCount() {
        if (this.commentCount > 0) {
            this.commentCount--;
        }
    }

    public void incrementShareCount() {
        this.shareCount++;
    }

    /**
     * Publish bài viết
     */
    public void publish() {
        this.status = PostStatus.ACTIVE;
        this.publishedAt = LocalDateTime.now();
    }

    /**
     * Ẩn bài viết
     */
    public void hide() {
        this.status = PostStatus.HIDDEN;
    }

    /**
     * Kiểm tra bài viết có visible với user không
     */
    public boolean isVisibleTo(User user) {
        if (visibility == Visibility.PUBLIC) {
            return true;
        }
        if (visibility == Visibility.PRIVATE) {
            return user != null && user.getId().equals(author.getId());
        }
        // For FOLLOWERS, need to check if user follows author
        return false;
    }

    // ==================== Alias/Helper Methods for Controller Compatibility ====================

    /**
     * Get excerpt (alias for summary)
     */
    public String getExcerpt() {
        return this.summary;
    }

    /**
     * Get thumbnail (alias for coverImage)
     */
    public String getThumbnail() {
        return this.coverImage;
    }

    /**
     * Get tags (stub - returns empty list until tags feature is implemented)
     */
    public java.util.List<String> getTags() {
        return java.util.Collections.emptyList();
    }

    /**
     * Get featured status (alias for isPinned)
     */
    public Boolean getFeatured() {
        return this.isPinned;
    }

    /**
     * Get view count as int (for controller compatibility)
     */
    public int getViewCount() {
        return this.viewCount != null ? this.viewCount.intValue() : 0;
    }

    /**
     * Get like count as int (for controller compatibility)
     */
    public int getLikeCount() {
        return this.likeCount != null ? this.likeCount.intValue() : 0;
    }

    /**
     * Get comment count as int (for controller compatibility)
     */
    public int getCommentCount() {
        return this.commentCount != null ? this.commentCount.intValue() : 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BlogPost blogPost = (BlogPost) o;
        return id != null && Objects.equals(id, blogPost.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "BlogPost{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", status=" + status +
                '}';
    }
}
