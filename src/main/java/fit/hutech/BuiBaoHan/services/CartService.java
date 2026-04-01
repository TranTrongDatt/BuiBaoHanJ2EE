package fit.hutech.BuiBaoHan.services;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import fit.hutech.BuiBaoHan.daos.Cart;
import fit.hutech.BuiBaoHan.daos.Item;
import fit.hutech.BuiBaoHan.entities.Invoice;
import fit.hutech.BuiBaoHan.entities.ItemInvoice;
import fit.hutech.BuiBaoHan.entities.User;
import fit.hutech.BuiBaoHan.repositories.IBookRepository;
import fit.hutech.BuiBaoHan.repositories.IInvoiceRepository;
import fit.hutech.BuiBaoHan.repositories.IItemInvoiceRepository;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Validated
@Transactional(isolation = Isolation.SERIALIZABLE,
        rollbackFor = {Exception.class, Throwable.class})
public class CartService {

    private static final String CART_SESSION_KEY = "cart";
    private final IInvoiceRepository invoiceRepository;
    private final IItemInvoiceRepository itemInvoiceRepository;
    private final IBookRepository bookRepository;
    private final fit.hutech.BuiBaoHan.repositories.ICartRepository cartRepository;
    private final fit.hutech.BuiBaoHan.repositories.ICartItemRepository cartItemRepository;
    private final fit.hutech.BuiBaoHan.repositories.IUserRepository userRepository;
    @Lazy
    private final WishlistService wishlistService;

    public Cart getCart(@NotNull HttpSession session) {
        return Optional.ofNullable((Cart) session.getAttribute(CART_SESSION_KEY))
                .orElseGet(() -> {
                    Cart cart = new Cart();
                    session.setAttribute(CART_SESSION_KEY, cart);
                    return cart;
                });
    }

    public void updateCart(@NotNull HttpSession session, Cart cart) {
        session.setAttribute(CART_SESSION_KEY, cart);
    }

    public void removeCart(@NotNull HttpSession session) {
        session.removeAttribute(CART_SESSION_KEY);
    }

    public int getSumQuantity(@NotNull HttpSession session) {
        return getCart(session).getCartItems().stream()
                .mapToInt(Item::getQuantity)
                .sum();
    }

    public double getSumPrice(@NotNull HttpSession session) {
        return getCart(session).getCartItems().stream()
                .mapToDouble(item -> item.getPrice()
                * item.getQuantity())
                .sum();
    }

    public void saveCart(@NotNull HttpSession session) {
        var cart = getCart(session);
        if (cart.getCartItems().isEmpty()) {
            return;
        }
        var invoice = new Invoice();
        invoice.setInvoiceDate(new Date(new Date().getTime()));
        invoice.setPrice(getSumPrice(session));
        invoiceRepository.save(invoice);
        cart.getCartItems().forEach(item -> {
            var items = new ItemInvoice();
            items.setInvoice(invoice);
            items.setQuantity(item.getQuantity());
            items.setBook(bookRepository.findById(item.getBookId())
                    .orElseThrow());
            itemInvoiceRepository.save(items);
        });
        removeCart(session);
    }

    /**
     * Thêm sách vào giỏ hàng (database-backed cart)
     */
    public fit.hutech.BuiBaoHan.entities.CartItem addToCart(Long userId, Long bookId, int quantity) {
        // Lấy hoặc tạo cart cho user
        fit.hutech.BuiBaoHan.entities.Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    fit.hutech.BuiBaoHan.entities.User user = userRepository.findById(userId)
                            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy user ID: " + userId));
                    fit.hutech.BuiBaoHan.entities.Cart newCart = fit.hutech.BuiBaoHan.entities.Cart.builder()
                            .user(user)
                            .build();
                    return cartRepository.save(newCart);
                });

        // Kiểm tra sách có trong giỏ chưa
        Optional<fit.hutech.BuiBaoHan.entities.CartItem> existingItem = 
                cartItemRepository.findByCartIdAndBookId(cart.getId(), bookId);

        if (existingItem.isPresent()) {
            // Tăng số lượng
            fit.hutech.BuiBaoHan.entities.CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + quantity);
            return cartItemRepository.save(item);
        } else {
            // Tạo mới
            fit.hutech.BuiBaoHan.entities.Book book = bookRepository.findById(bookId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sách ID: " + bookId));

            fit.hutech.BuiBaoHan.entities.CartItem newItem = fit.hutech.BuiBaoHan.entities.CartItem.builder()
                    .cart(cart)
                    .book(book)
                    .quantity(quantity)
                    .build();
            return cartItemRepository.save(newItem);
        }
    }

    // ==================== User Wrapper Methods (Database-backed) ====================

    /**
     * Lấy cart items của user (wrapper cho User object)
     */
    @Transactional(readOnly = true)
    public List<fit.hutech.BuiBaoHan.entities.CartItem> getCartItems(User user) {
        return cartRepository.findByUserId(user.getId())
                .map(cart -> cartItemRepository.findByCartIdWithBooks(cart.getId()))
                .orElse(Collections.emptyList());
    }

    /**
     * Tính tổng tiền giỏ hàng của user (wrapper cho User object)
     */
    @Transactional(readOnly = true)
    public BigDecimal getCartTotal(User user) {
        List<fit.hutech.BuiBaoHan.entities.CartItem> items = getCartItems(user);
        return items.stream()
                .map(item -> {
                    BigDecimal price = item.getBook().getPrice() != null 
                            ? item.getBook().getPrice() 
                            : BigDecimal.ZERO;
                    return price.multiply(BigDecimal.valueOf(item.getQuantity()));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Đếm số lượng items trong giỏ hàng của user (wrapper cho User object)
     */
    @Transactional(readOnly = true)
    public int getCartItemCount(User user) {
        return getCartItems(user).size();
    }

    /**
     * Thêm sách vào giỏ hàng (wrapper cho User object)
     */
    public fit.hutech.BuiBaoHan.entities.CartItem addToCart(User user, Long bookId, int quantity) {
        return addToCart(user.getId(), bookId, quantity);
    }

    /**
     * Cập nhật số lượng item trong giỏ hàng (wrapper cho User object)
     */
    public fit.hutech.BuiBaoHan.entities.CartItem updateQuantity(User user, Long itemId, int quantity) {
        fit.hutech.BuiBaoHan.entities.CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cart item ID: " + itemId));

        // Kiểm tra quyền sở hữu
        if (!item.getCart().getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Bạn không có quyền cập nhật item này");
        }

        if (quantity <= 0) {
            cartItemRepository.delete(item);
            return item;
        }

        item.setQuantity(quantity);
        return cartItemRepository.save(item);
    }

    /**
     * Xóa item khỏi giỏ hàng (wrapper cho User object)
     */
    public void removeFromCart(User user, Long itemId) {
        fit.hutech.BuiBaoHan.entities.CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cart item ID: " + itemId));

        // Kiểm tra quyền sở hữu
        if (!item.getCart().getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Bạn không có quyền xóa item này");
        }

        cartItemRepository.delete(item);
    }

    /**
     * Xóa toàn bộ giỏ hàng của user (wrapper cho User object)
     */
    public void clearCart(User user) {
        cartRepository.findByUserId(user.getId()).ifPresent(cart -> {
            cartItemRepository.deleteByCartId(cart.getId());
        });
    }

    /**
     * Chuyển item từ giỏ hàng sang wishlist (wrapper cho User object)
     */
    public void moveToWishlist(User user, Long itemId) {
        fit.hutech.BuiBaoHan.entities.CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cart item ID: " + itemId));

        // Kiểm tra quyền sở hữu
        if (!item.getCart().getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Bạn không có quyền thao tác item này");
        }

        // Thêm vào wishlist
        wishlistService.addToWishlist(user.getId(), item.getBook().getId());

        // Xóa khỏi cart
        cartItemRepository.delete(item);
    }
}
