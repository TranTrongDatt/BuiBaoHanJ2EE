package fit.hutech.BuiBaoHan.controllers;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import fit.hutech.BuiBaoHan.constants.PaymentMethod;
import fit.hutech.BuiBaoHan.entities.CartItem;
import fit.hutech.BuiBaoHan.entities.Order;
import fit.hutech.BuiBaoHan.entities.User;
import fit.hutech.BuiBaoHan.security.AuthResolver;
import fit.hutech.BuiBaoHan.services.BookService;
import fit.hutech.BuiBaoHan.services.CartService;
import fit.hutech.BuiBaoHan.services.OrderService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
@Slf4j
public class CartController {

    private final CartService cartService;
    private final BookService bookService;
    private final OrderService orderService;
    private final AuthResolver authResolver;

    // ==================== Helper: check logged-in ====================
    private boolean isLoggedIn(Object principal) {
        return authResolver.isAuthenticated(principal);
    }

    // ==================== Show Cart ====================
    @GetMapping
    public String showCart(HttpSession session,
            @AuthenticationPrincipal Object principal,
            @NotNull Model model) {
        if (isLoggedIn(principal)) {
            User user = authResolver.resolveUser(principal);
            List<CartItem> items = cartService.getCartItems(user);
            BigDecimal totalPrice = cartService.getCartTotal(user);
            int totalQuantity = items.stream().mapToInt(CartItem::getQuantity).sum();
            model.addAttribute("cartItems", items);
            model.addAttribute("totalPrice", totalPrice);
            model.addAttribute("totalQuantity", totalQuantity);
            model.addAttribute("useDbCart", true);
        } else {
            var cart = cartService.getCart(session);
            model.addAttribute("cart", cart);
            model.addAttribute("totalPrice", cartService.getSumPrice(session));
            model.addAttribute("totalQuantity", cartService.getSumQuantity(session));
            model.addAttribute("useDbCart", false);
        }
        return "book/cart";
    }

    // ==================== Add to Cart ====================
    @PostMapping("/add")
    public String addToCart(HttpSession session,
            @AuthenticationPrincipal Object principal,
            @RequestParam Long bookId,
            @RequestParam(defaultValue = "1") int quantity,
            RedirectAttributes redirectAttributes) {
        log.info("Add to cart: bookId={}, quantity={}", bookId, quantity);
        if (isLoggedIn(principal)) {
            User user = authResolver.resolveUser(principal);
            try {
                cartService.addToCart(user, bookId, quantity);
                redirectAttributes.addFlashAttribute("successMessage", "Đã thêm sách vào giỏ hàng!");
            } catch (Exception e) {
                log.warn("Failed to add to DB cart: {}", e.getMessage());
                redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            }
        } else {
            var book = bookService.getBookById(bookId);
            if (book.isPresent()) {
                var cart = cartService.getCart(session);
                var item = new fit.hutech.BuiBaoHan.daos.Item(
                        bookId, book.get().getTitle(), book.get().getPrice().doubleValue(), quantity);
                item.setCoverImage(book.get().getCoverImage());
                item.setSlug(book.get().getSlug());
                cart.addItems(item);
                cartService.updateCart(session, cart);
                redirectAttributes.addFlashAttribute("successMessage", "Đã thêm sách vào giỏ hàng!");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy sách với ID: " + bookId);
            }
        }
        return "redirect:/books";
    }

    // ==================== AJAX Add to Cart ====================
    @PostMapping("/add/ajax")
    @ResponseBody
    public Map<String, Object> addToCartAjax(HttpSession session,
            @AuthenticationPrincipal Object principal,
            @RequestParam Long bookId,
            @RequestParam(defaultValue = "1") int quantity) {
        Map<String, Object> response = new HashMap<>();
        log.info("AJAX add to cart: bookId={}, quantity={}", bookId, quantity);
        if (isLoggedIn(principal)) {
            User user = authResolver.resolveUser(principal);
            try {
                cartService.addToCart(user, bookId, quantity);
                List<CartItem> items = cartService.getCartItems(user);
                BigDecimal totalPrice = cartService.getCartTotal(user);
                int totalQuantity = items.stream().mapToInt(CartItem::getQuantity).sum();
                response.put("success", true);
                response.put("message", "Đã thêm sách vào giỏ hàng!");
                response.put("totalQuantity", totalQuantity);
                response.put("totalPrice", totalPrice);
                response.put("cartItemCount", items.size());
            } catch (Exception e) {
                response.put("success", false);
                response.put("message", e.getMessage());
            }
        } else {
            var book = bookService.getBookById(bookId);
            if (book.isPresent()) {
                var cart = cartService.getCart(session);
                var item = new fit.hutech.BuiBaoHan.daos.Item(
                        bookId, book.get().getTitle(), book.get().getPrice().doubleValue(), quantity);
                item.setCoverImage(book.get().getCoverImage());
                item.setSlug(book.get().getSlug());
                cart.addItems(item);
                cartService.updateCart(session, cart);
                response.put("success", true);
                response.put("message", "Đã thêm sách vào giỏ hàng!");
                response.put("totalQuantity", cartService.getSumQuantity(session));
                response.put("totalPrice", cartService.getSumPrice(session));
                response.put("cartItemCount", cart.getCartItems().size());
            } else {
                response.put("success", false);
                response.put("message", "Không tìm thấy sách với ID: " + bookId);
            }
        }
        return response;
    }

    // ==================== Buy Now ====================
    @PostMapping("/buy-now")
    public String buyNow(HttpSession session,
            @AuthenticationPrincipal Object principal,
            @RequestParam Long bookId,
            @RequestParam(defaultValue = "1") int quantity,
            RedirectAttributes redirectAttributes) {
        log.info("Buy now: bookId={}, quantity={}", bookId, quantity);
        if (isLoggedIn(principal)) {
            User user = authResolver.resolveUser(principal);
            try {
                cartService.addToCart(user, bookId, quantity);
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
                return "redirect:/books";
            }
        } else {
            var book = bookService.getBookById(bookId);
            if (book.isPresent()) {
                var cart = cartService.getCart(session);
                var item = new fit.hutech.BuiBaoHan.daos.Item(
                        bookId, book.get().getTitle(), book.get().getPrice().doubleValue(), quantity);
                item.setCoverImage(book.get().getCoverImage());
                item.setSlug(book.get().getSlug());
                cart.addItems(item);
                cartService.updateCart(session, cart);
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy sách với ID: " + bookId);
                return "redirect:/books";
            }
        }
        return "redirect:/cart";
    }

    // ==================== Remove from Cart ====================
    @GetMapping("/removeFromCart/{id}")
    public String removeFromCart(HttpSession session,
            @AuthenticationPrincipal Object principal,
            @PathVariable Long id) {
        if (isLoggedIn(principal)) {
            User user = authResolver.resolveUser(principal);
            try {
                cartService.removeFromCart(user, id);
            } catch (Exception e) {
                log.warn("Failed to remove from DB cart: {}", e.getMessage());
            }
        } else {
            var cart = cartService.getCart(session);
            cart.removeItems(id);
            cartService.updateCart(session, cart);
        }
        return "redirect:/cart";
    }

    // ==================== Update Cart (AJAX) ====================
    @GetMapping("/updateCart/{id}/{quantity}")
    @ResponseBody
    public Map<String, Object> updateCart(HttpSession session,
            @AuthenticationPrincipal Object principal,
            @PathVariable Long id,
            @PathVariable int quantity) {
        Map<String, Object> response = new HashMap<>();
        if (isLoggedIn(principal)) {
            User user = authResolver.resolveUser(principal);
            try {
                cartService.updateQuantity(user, id, quantity);
                List<CartItem> items = cartService.getCartItems(user);
                BigDecimal totalPrice = items.stream()
                        .map(CartItem::getSubtotal)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                int totalQuantity = items.stream().mapToInt(CartItem::getQuantity).sum();
                response.put("success", true);
                response.put("totalPrice", totalPrice);
                response.put("totalQuantity", totalQuantity);
                // Find updated item from eager-fetched list to avoid lazy loading
                items.stream()
                        .filter(i -> i.getId().equals(id))
                        .findFirst()
                        .ifPresent(item -> {
                            response.put("itemSubtotal", item.getSubtotal());
                            response.put("itemQuantity", item.getQuantity());
                        });
            } catch (Exception e) {
                response.put("success", false);
                response.put("message", e.getMessage());
            }
        } else {
            var cart = cartService.getCart(session);
            cart.updateItems(id, quantity);
            cartService.updateCart(session, cart);
            response.put("success", true);
            response.put("totalPrice", cartService.getSumPrice(session));
            response.put("totalQuantity", cartService.getSumQuantity(session));
            cart.getCartItems().stream()
                    .filter(item -> Objects.equals(item.getBookId(), id))
                    .findFirst()
                    .ifPresent(item -> {
                        response.put("itemSubtotal", item.getPrice() * item.getQuantity());
                        response.put("itemQuantity", item.getQuantity());
                    });
        }
        return response;
    }

    // ==================== Clear Cart ====================
    @GetMapping("/clearCart")
    public String clearCart(HttpSession session,
            @AuthenticationPrincipal Object principal) {
        if (isLoggedIn(principal)) {
            User user = authResolver.resolveUser(principal);
            cartService.clearCart(user);
        } else {
            cartService.removeCart(session);
        }
        return "redirect:/cart";
    }

    // ==================== Checkout ====================
    @GetMapping("/checkout")
    public String showCheckout(HttpSession session,
            @AuthenticationPrincipal Object principal,
            @NotNull Model model) {
        if (!authResolver.isAuthenticated(principal)) {
            return "redirect:/login";
        }
        User user = authResolver.resolveUser(principal);
        // Always use DB cart for checkout (user must be logged in)
        List<CartItem> items = cartService.getCartItems(user);
        if (items.isEmpty()) {
            return "redirect:/cart";
        }
        BigDecimal totalPrice = cartService.getCartTotal(user);
        int totalQuantity = items.stream().mapToInt(CartItem::getQuantity).sum();
        model.addAttribute("cartItems", items);
        model.addAttribute("totalPrice", totalPrice);
        model.addAttribute("totalQuantity", totalQuantity);
        // Pre-fill user info
        model.addAttribute("userName", user.getFullName() != null ? user.getFullName() : user.getUsername());
        model.addAttribute("userEmail", user.getEmail());
        model.addAttribute("userPhone", user.getPhone());
        model.addAttribute("userAddress", user.getAddress());
        return "book/checkout";
    }

    @PostMapping("/checkout")
    public String processCheckout(HttpSession session,
            @AuthenticationPrincipal Object principal,
            @RequestParam String receiverName,
            @RequestParam String receiverPhone,
            @RequestParam(required = false) String receiverEmail,
            @RequestParam(required = false, defaultValue = "") String province,
            @RequestParam(required = false, defaultValue = "") String district,
            @RequestParam(required = false, defaultValue = "") String ward,
            @RequestParam(required = false) String addressDetail,
            @RequestParam(defaultValue = "25000") BigDecimal shippingFee,
            @RequestParam(defaultValue = "COD") String paymentMethod,
            @RequestParam(required = false) String notes,
            RedirectAttributes redirectAttributes) {

        if (!authResolver.isAuthenticated(principal)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng đăng nhập để đặt hàng!");
            return "redirect:/login";
        }
        User user = authResolver.resolveUser(principal);

        List<CartItem> dbItems = cartService.getCartItems(user);
        if (dbItems.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Giỏ hàng trống!");
            return "redirect:/cart";
        }

        try {
            // Build full address
            String fullAddress = buildFullAddress(addressDetail, ward, district, province);

            // Parse payment method
            PaymentMethod pm;
            try {
                pm = PaymentMethod.valueOf(paymentMethod);
            } catch (IllegalArgumentException e) {
                pm = PaymentMethod.COD;
            }

            Order order = orderService.createOrderFromDbCart(
                    user, dbItems,
                    receiverName, receiverPhone, receiverEmail,
                    fullAddress, province, shippingFee,
                    pm, notes);

            // Clear DB cart after successful order
            cartService.clearCart(user);
            // Also clear session cart if any
            cartService.removeCart(session);

            // Redirect to confirmation page
            redirectAttributes.addFlashAttribute("order", order);
            return "redirect:/cart/order-confirmation/" + order.getOrderCode();

        } catch (Exception e) {
            log.error("Checkout error: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Đặt hàng thất bại: " + e.getMessage());
            return "redirect:/cart/checkout";
        }
    }

    @GetMapping("/order-confirmation/{orderCode}")
    public String orderConfirmation(@PathVariable String orderCode,
            @AuthenticationPrincipal Object principal,
            Model model) {
        var orderOpt = orderService.getOrderByCode(orderCode);
        if (orderOpt.isEmpty()) {
            return "redirect:/books";
        }
        var order = orderOpt.get();
        // Verify ownership
        if (!authResolver.isAuthenticated(principal) || !order.getUser().getId().equals(authResolver.resolveUserId(principal))) {
            return "redirect:/books";
        }
        model.addAttribute("order", order);
        return "book/order-confirmation";
    }

    private String buildFullAddress(String detail, String ward, String district, String province) {
        StringBuilder sb = new StringBuilder();
        if (detail != null && !detail.isBlank()) {
            sb.append(detail).append(", ");
        }
        if (ward != null && !ward.isBlank()) {
            sb.append(ward).append(", ");
        }
        if (district != null && !district.isBlank()) {
            sb.append(district).append(", ");
        }
        if (province != null && !province.isBlank()) {
            sb.append(province);
        }
        return sb.toString();
    }
}
