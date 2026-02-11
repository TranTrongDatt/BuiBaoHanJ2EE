package fit.hutech.BuiBaoHan.controllers.web;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import fit.hutech.BuiBaoHan.dto.PostCreateRequest;
import fit.hutech.BuiBaoHan.dto.PostUpdateRequest;
import fit.hutech.BuiBaoHan.entities.BlogPost;
import fit.hutech.BuiBaoHan.entities.User;
import fit.hutech.BuiBaoHan.security.AuthResolver;
import fit.hutech.BuiBaoHan.services.BlogPostService;
import fit.hutech.BuiBaoHan.services.BookService;
import fit.hutech.BuiBaoHan.services.CommentService;
import fit.hutech.BuiBaoHan.services.FileStorageService;
import fit.hutech.BuiBaoHan.services.LikeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Blog Web Controller
 */
@Controller
@RequestMapping("/blog")
@RequiredArgsConstructor
public class BlogController {

    private final BlogPostService blogPostService;
    private final CommentService commentService;
    private final LikeService likeService;
    private final BookService bookService;
    private final FileStorageService fileStorageService;
    private final AuthResolver authResolver;

    /**
     * Blog home page
     */
    @GetMapping
    public String blogHome(
            Model model,
            @PageableDefault(size = 12) Pageable pageable,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String search) {
        
        Page<BlogPost> posts;
        if (tag != null && !tag.isEmpty()) {
            posts = blogPostService.findByTag(tag, pageable);
            model.addAttribute("currentTag", tag);
        } else if (search != null && !search.isEmpty()) {
            posts = blogPostService.search(search, pageable);
            model.addAttribute("search", search);
        } else {
            posts = blogPostService.findPublished(pageable);
        }
        
        model.addAttribute("posts", posts);
        model.addAttribute("popularPosts", blogPostService.findPopular(5));
        model.addAttribute("tags", blogPostService.getAllTags());
        model.addAttribute("featuredPosts", blogPostService.findFeatured(3));
        
        return "blog/list";
    }

    /**
     * View blog post
     */
    @GetMapping("/{slug}")
    public String viewPost(
            @PathVariable String slug,
            @AuthenticationPrincipal Object principal,
            Model model) {
        
        return blogPostService.findBySlug(slug)
                .map(post -> {
                    blogPostService.incrementViewCount(post.getId());
                    
                    model.addAttribute("post", post);
                    model.addAttribute("comments", commentService.getCommentsByBlogPost(post.getId(), 
                            org.springframework.data.domain.Pageable.unpaged()));
                    model.addAttribute("relatedPosts", blogPostService.findRelated(post, 4));
                    
                    // User-specific data
                    if (authResolver.isAuthenticated(principal)) {
                        model.addAttribute("hasLiked", likeService.isLikedBlogPost(authResolver.resolveUserId(principal), post.getId()));
                    }
                    
                    return "blog/detail";
                })
                .orElse("redirect:/blog");
    }

    /**
     * New post page
     */
    @GetMapping("/new")
    @PreAuthorize("isAuthenticated()")
    public String newPostPage(Model model) {
        model.addAttribute("postForm", new PostForm());
        model.addAttribute("books", bookService.getAllBooksSimple());
        return "blog/editor";
    }

    /**
     * Create new post
     */
    @PostMapping("/new")
    @PreAuthorize("isAuthenticated()")
    public String createPost(
            @AuthenticationPrincipal Object principal,
            @Valid @ModelAttribute("postForm") PostForm form,
            BindingResult result,
            @RequestParam(required = false) MultipartFile thumbnailFile,
            Model model,
            RedirectAttributes redirectAttributes) {
        User user = authResolver.resolveUser(principal);
        
        if (result.hasErrors()) {
            model.addAttribute("books", bookService.getAllBooksSimple());
            return "blog/editor";
        }
        
        try {
            String thumbnailPath = null;
            if (thumbnailFile != null && !thumbnailFile.isEmpty()) {
                thumbnailPath = fileStorageService.storeImage(thumbnailFile, "blog");
            }
            
            PostCreateRequest request = new PostCreateRequest(
                    form.title(),
                    form.summary(),
                    form.content(),
                    thumbnailPath,
                    null, // mediaType - use default
                    null, // visibility - use default
                    true, // allowComments
                    form.bookId()
            );
            
            BlogPost post = blogPostService.create(user, request);
            
            if (form.publish()) {
                redirectAttributes.addFlashAttribute("success", "Post published successfully!");
            } else {
                redirectAttributes.addFlashAttribute("success", "Draft saved successfully!");
            }
            
            return "redirect:/blog/" + post.getSlug();
        } catch (java.io.IOException | RuntimeException e) {
            model.addAttribute("error", "Error creating post: " + e.getMessage());
            model.addAttribute("books", bookService.getAllBooksSimple());
            return "blog/editor";
        }
    }

    /**
     * Edit post page
     */
    @GetMapping("/{slug}/edit")
    @PreAuthorize("isAuthenticated()")
    public String editPostPage(
            @PathVariable String slug,
            @AuthenticationPrincipal Object principal,
            Model model,
            RedirectAttributes redirectAttributes) {
        User user = authResolver.resolveUser(principal);
        
        return blogPostService.findBySlug(slug)
                .filter(post -> post.getAuthor().getId().equals(user.getId()) || user.isAdmin())
                .map(post -> {
                    model.addAttribute("post", post);
                    model.addAttribute("postForm", PostForm.from(post));
                    model.addAttribute("books", bookService.getAllBooksSimple());
                    return "blog/editor";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Post not found or access denied");
                    return "redirect:/blog";
                });
    }

    /**
     * Update post
     */
    @PostMapping("/{slug}/edit")
    @PreAuthorize("isAuthenticated()")
    public String updatePost(
            @PathVariable String slug,
            @AuthenticationPrincipal Object principal,
            @Valid @ModelAttribute("postForm") PostForm form,
            BindingResult result,
            @RequestParam(required = false) MultipartFile thumbnailFile,
            Model model,
            RedirectAttributes redirectAttributes) {
        User user = authResolver.resolveUser(principal);
        
        if (result.hasErrors()) {
            model.addAttribute("books", bookService.getAllBooksSimple());
            return "blog/editor";
        }
        
        try {
            BlogPost post = blogPostService.findBySlug(slug)
                    .orElseThrow(() -> new IllegalArgumentException("Post not found"));
            
            String coverImagePath = post.getCoverImage();
            if (thumbnailFile != null && !thumbnailFile.isEmpty()) {
                coverImagePath = fileStorageService.storeImage(thumbnailFile, "blog");
            }
            
            PostUpdateRequest request = new PostUpdateRequest(
                    form.title(),
                    form.summary(),
                    form.content(),
                    coverImagePath,
                    null, // mediaType - keep existing
                    null, // visibility - keep existing
                    true, // allowComments
                    null, // isPinned - keep existing
                    form.bookId()
            );
            
            post = blogPostService.update(post.getId(), user, request);
            
            redirectAttributes.addFlashAttribute("success", "Post updated successfully!");
            return "redirect:/blog/" + post.getSlug();
        } catch (java.io.IOException | RuntimeException e) {
            model.addAttribute("error", "Error updating post: " + e.getMessage());
            model.addAttribute("books", bookService.getAllBooksSimple());
            return "blog/editor";
        }
    }

    /**
     * Delete post
     */
    @PostMapping("/{slug}/delete")
    @PreAuthorize("isAuthenticated()")
    public String deletePost(
            @PathVariable String slug,
            @AuthenticationPrincipal Object principal,
            RedirectAttributes redirectAttributes) {
        User user = authResolver.resolveUser(principal);
        
        try {
            BlogPost post = blogPostService.findBySlug(slug)
                    .orElseThrow(() -> new IllegalArgumentException("Post not found"));
            
            blogPostService.delete(post.getId(), user);
            redirectAttributes.addFlashAttribute("success", "Post deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting post: " + e.getMessage());
        }
        return "redirect:/blog";
    }

    /**
     * My posts page
     */
    @GetMapping("/my-posts")
    @PreAuthorize("isAuthenticated()")
    public String myPosts(
            @AuthenticationPrincipal Object principal,
            @PageableDefault(size = 12) Pageable pageable,
            @RequestParam(required = false) String status,
            Model model) {
        User user = authResolver.resolveUser(principal);
        
        Page<BlogPost> posts = (status != null && !status.isEmpty())
                ? blogPostService.findByUserAndStatus(user, status, pageable)
                : blogPostService.findByUser(user, pageable);
        
        model.addAttribute("posts", posts);
        model.addAttribute("status", status);
        
        return "blog/my-posts";
    }

    /**
     * User's public posts
     */
    @GetMapping("/author/{username}")
    public String authorPosts(
            @PathVariable String username,
            @PageableDefault(size = 12) Pageable pageable,
            Model model) {
        
        Page<BlogPost> posts = blogPostService.findByAuthorUsername(username, pageable);
        model.addAttribute("posts", posts);
        model.addAttribute("authorUsername", username);
        
        return "blog/author";
    }

    // ==================== Inner Records ====================

    public record PostForm(
            String title,
            String content,
            String summary,
            Long bookId,
            boolean publish
    ) {
        public PostForm() {
            this("", "", "", null, false);
        }

        public static PostForm from(BlogPost post) {
            return new PostForm(
                    post.getTitle(),
                    post.getContent(),
                    post.getSummary(),
                    post.getBook() != null ? post.getBook().getId() : null,
                    post.getStatus() == fit.hutech.BuiBaoHan.constants.PostStatus.PUBLISHED
            );
        }
    }
}
