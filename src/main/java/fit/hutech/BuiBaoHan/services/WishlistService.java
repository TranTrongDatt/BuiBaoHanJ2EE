package fit.hutech.BuiBaoHan.services;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fit.hutech.BuiBaoHan.entities.Book;
import fit.hutech.BuiBaoHan.entities.User;
import fit.hutech.BuiBaoHan.entities.Wishlist;
import fit.hutech.BuiBaoHan.repositories.IBookRepository;
import fit.hutech.BuiBaoHan.repositories.IUserRepository;
import fit.hutech.BuiBaoHan.repositories.IWishlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service quản lý Danh sách yêu thích
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class WishlistService {

    private final IWishlistRepository wishlistRepository;
    private final IUserRepository userRepository;
    private final IBookRepository bookRepository;

    /**
     * Lấy wishlist của user (eager fetch book + author)
     */
    @Transactional(readOnly = true)
    public List<Wishlist> getWishlist(Long userId) {
        return wishlistRepository.findByUserIdWithBooksAndAuthor(userId);
    }

    /**
     * Lấy wishlist phân trang (eager fetch book + author)
     */
    @Transactional(readOnly = true)
    public Page<Wishlist> getWishlistPaged(Long userId, Pageable pageable) {
        return wishlistRepository.findByUserIdWithBooksAndAuthorPaged(userId, pageable);
    }

    /**
     * Đếm số item trong wishlist
     */
    @Transactional(readOnly = true)
    public int getWishlistCount(Long userId) {
        return wishlistRepository.countByUserId(userId);
    }

    /**
     * Kiểm tra sách có trong wishlist không
     */
    @Transactional(readOnly = true)
    public boolean isInWishlist(Long userId, Long bookId) {
        return wishlistRepository.existsByUserIdAndBookId(userId, bookId);
    }

    /**
     * Thêm sách vào wishlist
     */
    public Wishlist addToWishlist(Long userId, Long bookId) {
        // Kiểm tra đã có chưa
        if (wishlistRepository.existsByUserIdAndBookId(userId, bookId)) {
            throw new IllegalStateException("Sách đã có trong danh sách yêu thích");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy user ID: " + userId));

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sách ID: " + bookId));

        Wishlist wishlist = Wishlist.builder()
                .user(user)
                .book(book)
                .build();

        Wishlist saved = wishlistRepository.save(wishlist);
        log.info("Added book {} to wishlist for user {}", bookId, userId);
        return saved;
    }

    /**
     * Xóa sách khỏi wishlist
     */
    public void removeFromWishlist(Long userId, Long bookId) {
        Optional<Wishlist> wishlist = wishlistRepository.findByUserIdAndBookId(userId, bookId);
        
        if (wishlist.isEmpty()) {
            throw new IllegalArgumentException("Sách không có trong danh sách yêu thích");
        }

        wishlistRepository.delete(wishlist.get());
        log.info("Removed book {} from wishlist for user {}", bookId, userId);
    }

    /**
     * Toggle wishlist (thêm nếu chưa có, xóa nếu đã có)
     */
    public boolean toggleWishlist(Long userId, Long bookId) {
        if (isInWishlist(userId, bookId)) {
            removeFromWishlist(userId, bookId);
            return false; // Đã xóa
        } else {
            addToWishlist(userId, bookId);
            return true; // Đã thêm
        }
    }

    /**
     * Xóa toàn bộ wishlist
     */
    public void clearWishlist(Long userId) {
        wishlistRepository.deleteByUserId(userId);
        log.info("Cleared wishlist for user {}", userId);
    }

    /**
     * Chuyển item từ wishlist sang giỏ hàng
     */
    public void moveToCart(Long userId, Long bookId, CartService cartService) {
        if (!isInWishlist(userId, bookId)) {
            throw new IllegalArgumentException("Sách không có trong danh sách yêu thích");
        }

        // Thêm vào giỏ
        cartService.addToCart(userId, bookId, 1);
        
        // Xóa khỏi wishlist
        removeFromWishlist(userId, bookId);
        
        log.info("Moved book {} from wishlist to cart for user {}", bookId, userId);
    }

    /**
     * Chuyển tất cả items từ wishlist sang giỏ hàng
     */
    public int moveAllToCart(Long userId, CartService cartService) {
        List<Wishlist> items = wishlistRepository.findByUserIdOrderByCreatedAtDesc(userId);
        int movedCount = 0;

        for (Wishlist item : items) {
            try {
                cartService.addToCart(userId, item.getBook().getId(), 1);
                wishlistRepository.delete(item);
                movedCount++;
            } catch (Exception e) {
                log.warn("Failed to move book {} to cart: {}", item.getBook().getId(), e.getMessage());
            }
        }

        log.info("Moved {} items from wishlist to cart for user {}", movedCount, userId);
        return movedCount;
    }

    /**
     * Lấy sách được yêu thích nhiều nhất
     */
    @Transactional(readOnly = true)
    public List<Book> getMostWishedBooks(int limit) {
        return wishlistRepository.findMostWishedBooks(limit);
    }

    /**
     * Đếm số người yêu thích một sách
     */
    @Transactional(readOnly = true)
    public long countWishlistByBook(Long bookId) {
        return wishlistRepository.countByBookId(bookId);
    }
}
