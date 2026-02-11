package fit.hutech.BuiBaoHan.controllers.web;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

import fit.hutech.BuiBaoHan.entities.User;
import fit.hutech.BuiBaoHan.security.AuthResolver;
import fit.hutech.BuiBaoHan.services.FileStorageService;
import fit.hutech.BuiBaoHan.services.NotificationService;
import fit.hutech.BuiBaoHan.services.OrderService;
import fit.hutech.BuiBaoHan.services.PdfService;
import fit.hutech.BuiBaoHan.services.UserService;
import fit.hutech.BuiBaoHan.services.WishlistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * User Profile Controller
 */
@Controller
@RequestMapping("/profile")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;
    private final OrderService orderService;
    private final FileStorageService fileStorageService;
    private final PdfService pdfService;
    private final NotificationService notificationService;
    private final WishlistService wishlistService;
    private final AuthResolver authResolver;

    /**
     * Profile page
     */
    @GetMapping
    public String profile(@AuthenticationPrincipal Object principal, Model model) {
        Long userId = authResolver.resolveUserId(principal);
        User user = authResolver.resolveUser(principal);
        model.addAttribute("user", user);
        model.addAttribute("recentOrders", orderService.findByUserRecentById(userId, 5));
        return "profile/index";
    }

    /**
     * Edit profile page
     */
    @GetMapping("/edit")
    public String editProfile(@AuthenticationPrincipal Object principal, Model model) {
        User user = authResolver.resolveUser(principal);
        model.addAttribute("user", user);
        model.addAttribute("profileForm", ProfileForm.from(user));
        return "profile/edit";
    }

    /**
     * Update profile
     */
    @PostMapping("/edit")
    public String updateProfile(
            @AuthenticationPrincipal Object principal,
            @Valid @ModelAttribute("profileForm") ProfileForm form,
            BindingResult result,
            @RequestParam(required = false) MultipartFile avatarFile,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        Long userId = authResolver.resolveUserId(principal);
        User user = authResolver.resolveUser(principal);
        
        if (result.hasErrors()) {
            model.addAttribute("user", user);
            return "profile/edit";
        }
        
        try {
            // Handle avatar upload
            String avatarPath = null;
            if (avatarFile != null && !avatarFile.isEmpty()) {
                avatarPath = fileStorageService.storeImage(avatarFile, "avatars");
            }
            
            userService.updateProfile(userId, form.fullName(), form.phone(), 
                    form.address(), form.bio(), avatarPath);
            
            redirectAttributes.addFlashAttribute("success", "Profile updated successfully");
            return "redirect:/profile";
        } catch (java.io.IOException | RuntimeException e) {
            model.addAttribute("error", "Error updating profile: " + e.getMessage());
            model.addAttribute("user", user);
            return "profile/edit";
        }
    }

    /**
     * Change password page
     */
    @GetMapping("/change-password")
    public String changePasswordPage(Model model) {
        model.addAttribute("passwordForm", new PasswordForm("", "", ""));
        return "profile/change-password";
    }

    /**
     * Change password
     */
    @PostMapping("/change-password")
    public String changePassword(
            @AuthenticationPrincipal Object principal,
            @Valid @ModelAttribute("passwordForm") PasswordForm form,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        if (result.hasErrors()) {
            return "profile/change-password";
        }
        
        if (!form.newPassword().equals(form.confirmPassword())) {
            model.addAttribute("error", "Passwords do not match");
            return "profile/change-password";
        }
        
        try {
            userService.changePassword(authResolver.resolveUserId(principal), form.currentPassword(), form.newPassword());
            redirectAttributes.addFlashAttribute("success", "Password changed successfully");
            return "redirect:/profile";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "profile/change-password";
        }
    }

    /**
     * My orders page
     */
    @GetMapping("/orders")
    public String myOrders(
            @AuthenticationPrincipal Object principal,
            @RequestParam(required = false) String status,
            Model model) {
        
        Long userId = authResolver.resolveUserId(principal);
        if (status != null && !status.isEmpty()) {
            model.addAttribute("orders", orderService.findByUserAndStatusById(userId, status));
        } else {
            model.addAttribute("orders", orderService.findByUserAllById(userId));
        }
        model.addAttribute("status", status);
        
        return "profile/orders";
    }

    /**
     * Order detail page
     */
    @GetMapping("/orders/{orderId}")
    public String orderDetail(
            @AuthenticationPrincipal Object principal,
            @PathVariable Long orderId,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        Long userId = authResolver.resolveUserId(principal);
        return orderService.findByIdAndUserId(orderId, userId)
                .map(order -> {
                    model.addAttribute("order", order);
                    return "profile/order-detail";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Order not found");
                    return "redirect:/profile/orders";
                });
    }

    /**
     * View invoice as HTML page (để xem hóa đơn trực tiếp)
     */
    @GetMapping("/orders/{orderId}/invoice")
    public String viewInvoice(
            @AuthenticationPrincipal Object principal,
            @PathVariable Long orderId,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        Long userId = authResolver.resolveUserId(principal);
        return orderService.findByIdAndUserId(orderId, userId)
                .map(order -> {
                    model.addAttribute("order", order);
                    model.addAttribute("storeInfo", new PdfService.StoreInfo());
                    return "pdf/invoice";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Order not found");
                    return "redirect:/profile/orders";
                });
    }

    /**
     * Download PDF invoice for user's order
     */
    @GetMapping("/orders/{orderId}/pdf")
    public ResponseEntity<byte[]> downloadInvoicePdf(
            @AuthenticationPrincipal Object principal,
            @PathVariable Long orderId) {
        Long userId = authResolver.resolveUserId(principal);
        return orderService.findByIdAndUserId(orderId, userId)
                .map(order -> {
                    byte[] pdfBytes = pdfService.generateInvoicePdf(order);
                    
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_PDF);
                    headers.setContentDispositionFormData("attachment", 
                            "HoaDon_" + order.getOrderCode() + ".pdf");
                    headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
                    
                    return ResponseEntity.ok()
                            .headers(headers)
                            .body(pdfBytes);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Wishlist page
     */
    @GetMapping("/wishlist")
    public String wishlist(@AuthenticationPrincipal Object principal, Model model) {
        Long userId = authResolver.resolveUserId(principal);
        model.addAttribute("wishlistItems", wishlistService.getWishlist(userId));
        model.addAttribute("wishlistCount", wishlistService.getWishlistCount(userId));
        return "profile/wishlist";
    }

    /**
     * Reviews page - Đánh giá sản phẩm đã mua
     */
    @GetMapping("/reviews")
    public String reviews(@AuthenticationPrincipal Object principal, Model model) {
        // TODO: Implement review service to get pending and completed reviews
        model.addAttribute("pendingReviews", java.util.Collections.emptyList());
        model.addAttribute("completedReviews", java.util.Collections.emptyList());
        model.addAttribute("pendingCount", 0);
        return "profile/reviews";
    }

    /**
     * Addresses page
     */
    @GetMapping("/addresses")
    public String addresses(@AuthenticationPrincipal Object principal, Model model) {
        User user = authResolver.resolveUser(principal);
        model.addAttribute("addresses", userService.getAddresses(user));
        return "profile/addresses";
    }

    /**
     * Notification page - Danh sách thông báo của người dùng
     */
    @GetMapping("/notifications")
    public String notifications(
            @AuthenticationPrincipal Object principal,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        
        Long userId = authResolver.resolveUserId(principal);
        var pageable = org.springframework.data.domain.PageRequest.of(page, 20);
        var notifications = notificationService.getNotifications(userId, pageable);
        long unreadCount = notificationService.countUnread(userId);
        
        model.addAttribute("notifications", notifications);
        model.addAttribute("unreadCount", unreadCount);
        
        return "profile/notifications";
    }

    /**
     * Đánh dấu tất cả thông báo đã đọc
     */
    @PostMapping("/notifications/read-all")
    public String markAllNotificationsRead(
            @AuthenticationPrincipal Object principal,
            RedirectAttributes redirectAttributes) {
        
        notificationService.markAllAsRead(authResolver.resolveUserId(principal));
        redirectAttributes.addFlashAttribute("success", "Đã đánh dấu tất cả thông báo đã đọc");
        return "redirect:/profile/notifications";
    }

    /**
     * Đánh dấu 1 thông báo đã đọc
     */
    @PostMapping("/notifications/{id}/read")
    public String markNotificationRead(
            @AuthenticationPrincipal Object principal,
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {
        
        try {
            notificationService.markAsRead(id, authResolver.resolveUserId(principal));
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/profile/notifications";
    }

    /**
     * Xóa thông báo đã đọc
     */
    @PostMapping("/notifications/delete-read")
    public String deleteReadNotifications(
            @AuthenticationPrincipal Object principal,
            RedirectAttributes redirectAttributes) {
        
        notificationService.deleteReadNotifications(authResolver.resolveUserId(principal));
        redirectAttributes.addFlashAttribute("success", "Đã xóa thông báo đã đọc");
        return "redirect:/profile/notifications";
    }

    // ==================== Inner Records ====================

    public record ProfileForm(
            String fullName,
            String phone,
            String address,
            String bio
    ) {
        public static ProfileForm from(User user) {
            return new ProfileForm(
                    user.getFullName(),
                    user.getPhone(),
                    user.getAddress(),
                    user.getBio()
            );
        }
    }

    public record PasswordForm(
            String currentPassword,
            String newPassword,
            String confirmPassword
    ) {}
}
