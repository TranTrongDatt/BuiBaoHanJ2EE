package fit.hutech.BuiBaoHan.controllers.api;

import java.util.List;

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
import org.springframework.web.bind.annotation.RestController;

import fit.hutech.BuiBaoHan.dto.ApiResponse;
import fit.hutech.BuiBaoHan.dto.CartItemDto;
import fit.hutech.BuiBaoHan.entities.CartItem;
import fit.hutech.BuiBaoHan.entities.User;
import fit.hutech.BuiBaoHan.security.AuthResolver;
import fit.hutech.BuiBaoHan.services.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST API Controller for Shopping Cart
 */
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ApiCartController {

    private final CartService cartService;
    private final AuthResolver authResolver;

    /**
     * Get cart items
     */
    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart(@AuthenticationPrincipal Object principal) {
        User user = authResolver.resolveUser(principal);
        List<CartItem> items = cartService.getCartItems(user);
        List<CartItemResponse> itemResponses = items.stream()
                .map(CartItemResponse::from)
                .toList();
        
        CartResponse response = new CartResponse(
                itemResponses,
                cartService.getCartTotal(user),
                items.size()
        );
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get cart count
     */
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Integer>> getCartCount(@AuthenticationPrincipal Object principal) {
        User user = authResolver.resolveUser(principal);
        int count = cartService.getCartItemCount(user);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    /**
     * Add item to cart
     */
    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartItemResponse>> addToCart(
            @AuthenticationPrincipal Object principal,
            @Valid @RequestBody CartItemDto request) {
        User user = authResolver.resolveUser(principal);
        try {
            CartItem item = cartService.addToCart(user, request.bookId(), request.quantity());
            return ResponseEntity.ok(ApiResponse.success("Added to cart", CartItemResponse.from(item)));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Update cart item quantity
     */
    @PutMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<CartItemResponse>> updateQuantity(
            @AuthenticationPrincipal Object principal,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateQuantityRequest request) {
        User user = authResolver.resolveUser(principal);
        try {
            CartItem item = cartService.updateQuantity(user, itemId, request.quantity());
            return ResponseEntity.ok(ApiResponse.success("Quantity updated", CartItemResponse.from(item)));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Remove item from cart
     */
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<Void>> removeFromCart(
            @AuthenticationPrincipal Object principal,
            @PathVariable Long itemId) {
        User user = authResolver.resolveUser(principal);
        try {
            cartService.removeFromCart(user, itemId);
            return ResponseEntity.ok(ApiResponse.deleted());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Clear cart
     */
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> clearCart(@AuthenticationPrincipal Object principal) {
        User user = authResolver.resolveUser(principal);
        cartService.clearCart(user);
        return ResponseEntity.ok(ApiResponse.success("Cart cleared"));
    }

    /**
     * Move item to wishlist
     */
    @PostMapping("/items/{itemId}/move-to-wishlist")
    public ResponseEntity<ApiResponse<Void>> moveToWishlist(
            @AuthenticationPrincipal Object principal,
            @PathVariable Long itemId) {
        User user = authResolver.resolveUser(principal);
        try {
            cartService.moveToWishlist(user, itemId);
            return ResponseEntity.ok(ApiResponse.success("Moved to wishlist"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    // ==================== Inner Records ====================

    public record CartResponse(
            List<CartItemResponse> items,
            java.math.BigDecimal total,
            int itemCount
    ) {}

    public record CartItemResponse(
            Long id,
            Long bookId,
            String bookTitle,
            String bookSlug,
            String bookCoverImage,
            java.math.BigDecimal price,
            java.math.BigDecimal originalPrice,
            int quantity,
            java.math.BigDecimal subtotal,
            boolean available
    ) {
        public static CartItemResponse from(CartItem item) {
            var book = item.getBook();
            return new CartItemResponse(
                    item.getId(),
                    book.getId(),
                    book.getTitle(),
                    book.getSlug(),
                    book.getCoverImage(),
                    book.getPrice(),
                    book.getOriginalPrice(),
                    item.getQuantity(),
                    book.getPrice().multiply(java.math.BigDecimal.valueOf(item.getQuantity())),
                    book.getStock() >= item.getQuantity()
            );
        }
    }

    public record UpdateQuantityRequest(int quantity) {}
}
