package fit.hutech.BuiBaoHan.controllers.shipper;

import java.io.IOException;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import fit.hutech.BuiBaoHan.constants.OrderStatus;
import fit.hutech.BuiBaoHan.constants.ShipperStatus;
import fit.hutech.BuiBaoHan.dto.ApiResponse;
import fit.hutech.BuiBaoHan.entities.Order;
import fit.hutech.BuiBaoHan.entities.ShipperProfile;
import fit.hutech.BuiBaoHan.entities.User;
import fit.hutech.BuiBaoHan.repositories.IOrderRepository;
import fit.hutech.BuiBaoHan.repositories.IShipperProfileRepository;
import fit.hutech.BuiBaoHan.repositories.IUserRepository;
import fit.hutech.BuiBaoHan.services.CloudinaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Shipper Order Controller
 * Quản lý đơn hàng cho nhân viên giao hàng
 */
@Controller
@RequestMapping("/shipper/orders")
@PreAuthorize("hasAnyAuthority('ROLE_SHIPPER', 'ROLE_ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class ShipperOrderController {

    private final IUserRepository userRepository;
    private final IShipperProfileRepository shipperProfileRepository;
    private final IOrderRepository orderRepository;
    private final CloudinaryService cloudinaryService;

    /**
     * Danh sách đơn hàng của shipper
     */
    @GetMapping({"", "/"})
    public String myOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String status,
            Model model, 
            Authentication authentication) {
        
        User currentUser = getCurrentUser(authentication);
        if (currentUser == null) {
            return "redirect:/login";
        }

        Optional<ShipperProfile> profileOpt = shipperProfileRepository.findByUserId(currentUser.getId());
        if (profileOpt.isEmpty()) {
            return "redirect:/shipper/profile/setup";
        }

        ShipperProfile profile = profileOpt.get();
        Pageable pageable = PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Order> orders;
        if (status != null && !status.isEmpty()) {
            OrderStatus orderStatus = OrderStatus.valueOf(status);
            orders = orderRepository.findByShipperIdAndStatus(profile.getId(), orderStatus, pageable);
        } else {
            orders = orderRepository.findByShipperId(profile.getId(), pageable);
        }

        model.addAttribute("profile", profile);
        model.addAttribute("orders", orders);
        model.addAttribute("currentStatus", status);
        model.addAttribute("statuses", OrderStatus.values());

        // Thống kê
        model.addAttribute("pendingCount", orderRepository.countByShipperIdAndStatus(profile.getId(), OrderStatus.SHIPPING));
        model.addAttribute("completedCount", orderRepository.countByShipperIdAndStatus(profile.getId(), OrderStatus.DELIVERED));

        return "shipper/orders";
    }

    /**
     * Đơn hàng đang chờ nhận
     */
    @GetMapping("/available")
    public String availableOrders(
            @RequestParam(defaultValue = "0") int page,
            Model model, 
            Authentication authentication) {
        
        User currentUser = getCurrentUser(authentication);
        if (currentUser == null) {
            return "redirect:/login";
        }

        Optional<ShipperProfile> profileOpt = shipperProfileRepository.findByUserId(currentUser.getId());
        if (profileOpt.isEmpty()) {
            return "redirect:/shipper/profile/setup";
        }

        ShipperProfile profile = profileOpt.get();

        // Kiểm tra shipper có thể nhận đơn không
        if (!profile.canAcceptOrder()) {
            model.addAttribute("error", "Bạn không thể nhận đơn hàng lúc này. Hãy chuyển trạng thái sang Online.");
        }

        // Lấy các đơn đã xác nhận, chưa có shipper
        Pageable pageable = PageRequest.of(page, 20, Sort.by(Sort.Direction.ASC, "createdAt"));
        Page<Order> orders = orderRepository.findByStatusAndShipperIsNull(OrderStatus.CONFIRMED, pageable);

        model.addAttribute("profile", profile);
        model.addAttribute("orders", orders);

        return "shipper/available-orders";
    }

    /**
     * Chi tiết đơn hàng
     */
    @GetMapping("/{id}")
    public String orderDetail(@PathVariable Long id, Model model, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        if (currentUser == null) {
            return "redirect:/login";
        }

        Optional<ShipperProfile> profileOpt = shipperProfileRepository.findByUserId(currentUser.getId());
        if (profileOpt.isEmpty()) {
            return "redirect:/shipper/profile/setup";
        }

        ShipperProfile profile = profileOpt.get();
        Optional<Order> orderOpt = orderRepository.findById(id);

        if (orderOpt.isEmpty()) {
            return "redirect:/shipper/orders?error=notfound";
        }

        Order order = orderOpt.get();

        // Kiểm tra đơn có thuộc về shipper này không (nếu đã assign)
        if (order.hasShipper() && !order.getShipper().getId().equals(profile.getId())) {
            return "redirect:/shipper/orders?error=unauthorized";
        }

        model.addAttribute("profile", profile);
        model.addAttribute("order", order);

        return "shipper/order-detail";
    }

    /**
     * API nhận đơn hàng
     */
    @PostMapping("/{id}/accept")
    @Transactional
    @ResponseBody
    public ResponseEntity<ApiResponse<Order>> acceptOrder(
            @PathVariable Long id,
            Authentication authentication) {
        
        try {
            User currentUser = getCurrentUser(authentication);
            if (currentUser == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Vui lòng đăng nhập"));
            }

            Optional<ShipperProfile> profileOpt = shipperProfileRepository.findByUserId(currentUser.getId());
            if (profileOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Chưa có hồ sơ shipper"));
            }

            ShipperProfile profile = profileOpt.get();

            // Kiểm tra có thể nhận đơn không
            if (!profile.canAcceptOrder()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error("Bạn cần ở trạng thái Online để nhận đơn")
                );
            }

            Optional<Order> orderOpt = orderRepository.findById(id);
            if (orderOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Không tìm thấy đơn hàng"));
            }

            Order order = orderOpt.get();

            // Kiểm tra đơn có thể assign không
            if (!order.canAssignShipper()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error("Đơn hàng không thể nhận (đã có shipper hoặc chưa xác nhận)")
                );
            }

            // Assign shipper (sử dụng User từ profile)
            order.assignShipper(profile.getUser());
            orderRepository.save(order);

            // Cập nhật trạng thái shipper
            profile.setStatus(ShipperStatus.BUSY);
            shipperProfileRepository.save(profile);

            log.info("Shipper {} nhận đơn #{}", profile.getFullName(), order.getId());

            return ResponseEntity.ok(ApiResponse.success("Nhận đơn thành công!", order));

        } catch (Exception e) {
            log.error("Lỗi nhận đơn: ", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Có lỗi xảy ra"));
        }
    }

    /**
     * API bắt đầu giao hàng
     */
    @PostMapping("/{id}/start-delivery")
    @Transactional
    @ResponseBody
    public ResponseEntity<ApiResponse<Order>> startDelivery(
            @PathVariable Long id,
            Authentication authentication) {
        
        try {
            User currentUser = getCurrentUser(authentication);
            if (currentUser == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Vui lòng đăng nhập"));
            }

            Optional<ShipperProfile> profileOpt = shipperProfileRepository.findByUserId(currentUser.getId());
            if (profileOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Chưa có hồ sơ shipper"));
            }

            ShipperProfile profile = profileOpt.get();
            Optional<Order> orderOpt = orderRepository.findById(id);

            if (orderOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Không tìm thấy đơn hàng"));
            }

            Order order = orderOpt.get();

            // Kiểm tra quyền
            if (!order.hasShipper() || !order.getShipper().getId().equals(profile.getId())) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Bạn không có quyền với đơn hàng này"));
            }

            // Bắt đầu giao
            order.startDelivery();
            orderRepository.save(order);

            log.info("Shipper {} bắt đầu giao đơn #{}", profile.getFullName(), order.getId());

            return ResponseEntity.ok(ApiResponse.success("Bắt đầu giao hàng!", order));

        } catch (Exception e) {
            log.error("Lỗi bắt đầu giao hàng: ", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Có lỗi xảy ra"));
        }
    }

    /**
     * API hoàn thành giao hàng
     */
    @PostMapping("/{id}/complete")
    @Transactional
    @ResponseBody
    public ResponseEntity<ApiResponse<Order>> completeDelivery(
            @PathVariable Long id,
            @RequestParam(required = false) String notes,
            @RequestParam(required = false) MultipartFile proofImage,
            Authentication authentication) {
        
        try {
            User currentUser = getCurrentUser(authentication);
            if (currentUser == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Vui lòng đăng nhập"));
            }

            Optional<ShipperProfile> profileOpt = shipperProfileRepository.findByUserId(currentUser.getId());
            if (profileOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Chưa có hồ sơ shipper"));
            }

            ShipperProfile profile = profileOpt.get();
            Optional<Order> orderOpt = orderRepository.findById(id);

            if (orderOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Không tìm thấy đơn hàng"));
            }

            Order order = orderOpt.get();

            // Kiểm tra quyền
            if (!order.hasShipper() || !order.getShipper().getId().equals(profile.getId())) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Bạn không có quyền với đơn hàng này"));
            }

            // Upload ảnh chứng minh nếu có
            String proofImageUrl = null;
            if (proofImage != null && !proofImage.isEmpty()) {
                proofImageUrl = cloudinaryService.uploadImage(proofImage, "delivery-proofs");
            }

            // Hoàn thành giao hàng
            order.completeDelivery(notes, proofImageUrl);
            orderRepository.save(order);

            // Cập nhật thống kê shipper
            profile.recordDelivery(true);
            profile.setStatus(ShipperStatus.ONLINE);
            shipperProfileRepository.save(profile);

            log.info("Shipper {} hoàn thành đơn #{}", profile.getFullName(), order.getId());

            return ResponseEntity.ok(ApiResponse.success("Giao hàng thành công!", order));

        } catch (IOException | RuntimeException e) {
            log.error("Lỗi hoàn thành giao hàng: ", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Có lỗi xảy ra"));
        }
    }

    /**
     * API cập nhật trạng thái shipper
     */
    @PostMapping("/status")
    @ResponseBody
    public ResponseEntity<ApiResponse<ShipperProfile>> updateStatus(
            @RequestParam ShipperStatus status,
            Authentication authentication) {
        
        try {
            User currentUser = getCurrentUser(authentication);
            if (currentUser == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Vui lòng đăng nhập"));
            }

            Optional<ShipperProfile> profileOpt = shipperProfileRepository.findByUserId(currentUser.getId());
            if (profileOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Chưa có hồ sơ shipper"));
            }

            ShipperProfile profile = profileOpt.get();
            profile.setStatus(status);
            shipperProfileRepository.save(profile);

            return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái thành công!", profile));

        } catch (Exception e) {
            log.error("Lỗi cập nhật trạng thái: ", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Có lỗi xảy ra"));
        }
    }

    /**
     * Helper method lấy current user
     */
    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        String username = authentication.getName();
        return userRepository.findByUsername(username).orElse(null);
    }
}
