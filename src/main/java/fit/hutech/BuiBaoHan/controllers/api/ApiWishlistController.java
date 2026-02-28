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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fit.hutech.BuiBaoHan.dto.ApiResponse;
import fit.hutech.BuiBaoHan.dto.PageResponse;
import fit.hutech.BuiBaoHan.entities.Wishlist;
import fit.hutech.BuiBaoHan.security.AuthResolver;
import fit.hutech.BuiBaoHan.services.CartService;
import fit.hutech.BuiBaoHan.services.WishlistService;
import lombok.RequiredArgsConstructor;

/**
 * REST API Controller for Wishlist management
 */
@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ApiWishlistController {

    private final WishlistService wishlistService;
    private final CartService cartService;
    private final AuthResolver authResolver;

    /**
     * Get wishlist items (paginated)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<WishlistItemResponse>>> getWishlist(
            @AuthenticationPrincipal Object principal,
            @PageableDefault(size = 12) Pageable pageable) {
        
        Long userId = authResolver.resolveUserId(principal);
        Page<Wishlist> items = wishlistService.getWishlistPaged(userId, pageable);
        List<WishlistItemResponse> dtos = items.getContent().stream()
                .map(WishlistItemResponse::from)
                .toList();
        
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(items, dtos)));
    }

    /**
     * Get wishlist count
     */
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Integer>> getWishlistCount(@AuthenticationPrincipal Object principal) {
        int count = wishlistService.getWishlistCount(authResolver.resolveUserId(principal));
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    /**
     * Check if book is in wishlist
     */
    @GetMapping("/check/{bookId}")
    public ResponseEntity<ApiResponse<Boolean>> isInWishlist(
            @AuthenticationPrincipal Object principal,
            @PathVariable Long bookId) {
        boolean inWishlist = wishlistService.isInWishlist(authResolver.resolveUserId(principal), bookId);
        return ResponseEntity.ok(ApiResponse.success(inWishlist));
    }

    /**
     * Add book to wishlist
     */
    @PostMapping("/{bookId}")
    public ResponseEntity<ApiResponse<WishlistItemResponse>> addToWishlist(
            @AuthenticationPrincipal Object principal,
            @PathVariable Long bookId) {
        try {
            Wishlist item = wishlistService.addToWishlist(authResolver.resolveUserId(principal), bookId);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.created(WishlistItemResponse.from(item)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (IllegalStateException e) {
            // Already in wishlist
            return ResponseEntity.ok(ApiResponse.success("Already in wishlist"));
        }
    }

    /**
     * Remove book from wishlist
     */
    @DeleteMapping("/{bookId}")
    public ResponseEntity<ApiResponse<Void>> removeFromWishlist(
            @AuthenticationPrincipal Object principal,
            @PathVariable Long bookId) {
        try {
            wishlistService.removeFromWishlist(authResolver.resolveUserId(principal), bookId);
            return ResponseEntity.ok(ApiResponse.deleted());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Clear wishlist
     */
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> clearWishlist(@AuthenticationPrincipal Object principal) {
        wishlistService.clearWishlist(authResolver.resolveUserId(principal));
        return ResponseEntity.ok(ApiResponse.success("Wishlist cleared"));
    }

    /**
     * Move item to cart
     */
    @PostMapping("/{bookId}/move-to-cart")
    public ResponseEntity<ApiResponse<Void>> moveToCart(
            @AuthenticationPrincipal Object principal,
            @PathVariable Long bookId) {
        try {
            wishlistService.moveToCart(authResolver.resolveUserId(principal), bookId, cartService);
            return ResponseEntity.ok(ApiResponse.success("Moved to cart"));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Move all items to cart
     */
    @PostMapping("/move-all-to-cart")
    public ResponseEntity<ApiResponse<MoveToCartResult>> moveAllToCart(@AuthenticationPrincipal Object principal) {
        int movedCount = wishlistService.moveAllToCart(authResolver.resolveUserId(principal), cartService);
        return ResponseEntity.ok(ApiResponse.success("Items moved to cart", new MoveToCartResult(movedCount, 0, List.of())));
    }

    // ==================== Inner Records ====================

    public record WishlistItemResponse(
            Long id,
            Long bookId,
            String bookTitle,
            String bookSlug,
            String bookCoverImage,
            String authorName,
            java.math.BigDecimal price,
            java.math.BigDecimal originalPrice,
            boolean inStock,
            String addedAt
    ) {
        public static WishlistItemResponse from(Wishlist item) {
            var book = item.getBook();
            return new WishlistItemResponse(
                    item.getId(),
                    book.getId(),
                    book.getTitle(),
                    book.getSlug(),
                    book.getCoverImage(),
                    book.getAuthor() != null ? book.getAuthor().getName() : null,
                    book.getPrice(),
                    book.getOriginalPrice(),
                    book.getStock() > 0,
                    item.getCreatedAt().toString()
            );
        }
    }

    public record MoveToCartResult(int successCount, int failedCount, List<String> errors) {}
}
