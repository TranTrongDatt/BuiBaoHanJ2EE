package fit.hutech.BuiBaoHan.controllers.api;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fit.hutech.BuiBaoHan.dto.ApiResponse;
import fit.hutech.BuiBaoHan.dto.OrderCreateRequest;
import fit.hutech.BuiBaoHan.dto.OrderSummaryDto;
import fit.hutech.BuiBaoHan.dto.PageResponse;
import fit.hutech.BuiBaoHan.entities.Order;
import fit.hutech.BuiBaoHan.entities.OrderItem;
import fit.hutech.BuiBaoHan.entities.User;
import fit.hutech.BuiBaoHan.security.AuthResolver;
import fit.hutech.BuiBaoHan.services.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST API Controller for Order management
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class ApiOrderController {

    private final OrderService orderService;
    private final AuthResolver authResolver;

    // ==================== User Endpoints ====================

    /**
     * Get my orders
     */
    @GetMapping("/my-orders")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResponse<OrderSummaryDto>>> getMyOrders(
            @AuthenticationPrincipal Object principal,
            @PageableDefault(size = 10) Pageable pageable) {
        User user = authResolver.resolveUser(principal);
        
        Page<Order> orders = orderService.findByUser(user, pageable);
        List<OrderSummaryDto> dtos = orders.getContent().stream()
                .map(OrderSummaryDto::from)
                .toList();
        
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(orders, dtos)));
    }

    /**
     * Get order details
     */
    @GetMapping("/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<OrderDetailResponse>> getOrderDetails(
            @AuthenticationPrincipal Object principal,
            @PathVariable Long orderId) {
        User user = authResolver.resolveUser(principal);
        
        return orderService.findByIdAndUser(orderId, user)
                .map(order -> ResponseEntity.ok(ApiResponse.success(OrderDetailResponse.from(order))))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.notFound("Order")));
    }

    /**
     * Create order from cart
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<OrderDetailResponse>> createOrder(
            @AuthenticationPrincipal Object principal,
            @Valid @RequestBody OrderCreateRequest request) {
        User user = authResolver.resolveUser(principal);
        try {
            Order order = orderService.createFromCart(user, request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.created(OrderDetailResponse.from(order)));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Cancel order
     */
    @PostMapping("/{orderId}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<OrderSummaryDto>> cancelOrder(
            @AuthenticationPrincipal Object principal,
            @PathVariable Long orderId,
            @RequestParam(required = false) String reason) {
        User user = authResolver.resolveUser(principal);
        try {
            Order order = orderService.cancelOrder(orderId, user, reason);
            return ResponseEntity.ok(ApiResponse.success("Order cancelled", OrderSummaryDto.from(order)));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Track order
     */
    @GetMapping("/{orderId}/track")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<OrderTrackingResponse>> trackOrder(
            @AuthenticationPrincipal Object principal,
            @PathVariable Long orderId) {
        User user = authResolver.resolveUser(principal);
        
        return orderService.findByIdAndUser(orderId, user)
                .map(order -> ResponseEntity.ok(ApiResponse.success(OrderTrackingResponse.from(order))))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.notFound("Order")));
    }

    // ==================== Admin Endpoints ====================

    /**
     * Get all orders (Admin)
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<OrderSummaryDto>>> getAllOrders(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) String status) {
        
        Page<Order> orders = (status != null)
                ? orderService.findByStatus(status, pageable)
                : orderService.findAll(pageable);
        
        List<OrderSummaryDto> dtos = orders.getContent().stream()
                .map(OrderSummaryDto::from)
                .toList();
        
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(orders, dtos)));
    }

    /**
     * Update order status (Admin)
     */
    @PutMapping("/{orderId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderSummaryDto>> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam String status,
            @RequestParam(required = false) String note) {
        try {
            Order order = orderService.updateStatus(orderId, status, note);
            return ResponseEntity.ok(ApiResponse.success("Order status updated", OrderSummaryDto.from(order)));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Confirm order (Admin)
     */
    @PostMapping("/{orderId}/confirm")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderSummaryDto>> confirmOrder(@PathVariable Long orderId) {
        try {
            Order order = orderService.confirmOrder(orderId);
            return ResponseEntity.ok(ApiResponse.success("Order confirmed", OrderSummaryDto.from(order)));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Ship order (Admin)
     */
    @PostMapping("/{orderId}/ship")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderSummaryDto>> shipOrder(
            @PathVariable Long orderId,
            @RequestParam String trackingNumber) {
        try {
            Order order = orderService.shipOrder(orderId, trackingNumber);
            return ResponseEntity.ok(ApiResponse.success("Order shipped", OrderSummaryDto.from(order)));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Mark order as delivered (Admin)
     */
    @PostMapping("/{orderId}/deliver")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderSummaryDto>> deliverOrder(@PathVariable Long orderId) {
        try {
            Order order = orderService.deliverOrder(orderId);
            return ResponseEntity.ok(ApiResponse.success("Order delivered", OrderSummaryDto.from(order)));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    // ==================== Inner Records ====================

    public record OrderDetailResponse(
            Long id,
            String orderNumber,
            String status,
            java.math.BigDecimal subtotal,
            java.math.BigDecimal shippingFee,
            java.math.BigDecimal discount,
            java.math.BigDecimal total,
            String shippingName,
            String shippingPhone,
            String shippingAddress,
            String paymentMethod,
            String paymentStatus,
            String note,
            String trackingNumber,
            String createdAt,
            String confirmedAt,
            String shippedAt,
            String deliveredAt,
            List<OrderItemResponse> items
    ) {
        public static OrderDetailResponse from(Order order) {
            List<OrderItemResponse> items = order.getOrderItems().stream()
                    .map(OrderItemResponse::from)
                    .toList();
            
            return new OrderDetailResponse(
                    order.getId(),
                    order.getOrderNumber(),
                    order.getStatus().name(),
                    order.getSubtotal(),
                    order.getShippingFee(),
                    order.getDiscount(),
                    order.getTotal(),
                    order.getShippingName(),
                    order.getShippingPhone(),
                    order.getShippingAddress(),
                    order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null,
                    order.getPaymentStatus() != null ? order.getPaymentStatus().name() : null,
                    order.getNote(),
                    order.getTrackingNumber(),
                    order.getCreatedAt().toString(),
                    order.getConfirmedAt() != null ? order.getConfirmedAt().toString() : null,
                    order.getShippedAt() != null ? order.getShippedAt().toString() : null,
                    order.getDeliveredAt() != null ? order.getDeliveredAt().toString() : null,
                    items
            );
        }
    }

    public record OrderItemResponse(
            Long id,
            Long bookId,
            String bookTitle,
            String bookCoverImage,
            int quantity,
            java.math.BigDecimal price,
            java.math.BigDecimal subtotal
    ) {
        public static OrderItemResponse from(OrderItem item) {
            return new OrderItemResponse(
                    item.getId(),
                    item.getBook().getId(),
                    item.getBook().getTitle(),
                    item.getBook().getCoverImage(),
                    item.getQuantity(),
                    item.getPrice(),
                    item.getSubtotal()
            );
        }
    }

    public record OrderTrackingResponse(
            Long id,
            String orderNumber,
            String status,
            String trackingNumber,
            List<TrackingEvent> events
    ) {
        public static OrderTrackingResponse from(Order order) {
            List<TrackingEvent> events = new java.util.ArrayList<>();
            
            events.add(new TrackingEvent("Order placed", order.getCreatedAt().toString(), true));
            
            if (order.getConfirmedAt() != null) {
                events.add(new TrackingEvent("Order confirmed", order.getConfirmedAt().toString(), true));
            }
            if (order.getShippedAt() != null) {
                events.add(new TrackingEvent("Order shipped", order.getShippedAt().toString(), true));
            }
            if (order.getDeliveredAt() != null) {
                events.add(new TrackingEvent("Order delivered", order.getDeliveredAt().toString(), true));
            }
            
            return new OrderTrackingResponse(
                    order.getId(),
                    order.getOrderNumber(),
                    order.getStatus().name(),
                    order.getTrackingNumber(),
                    events
            );
        }
    }

    public record TrackingEvent(String title, String timestamp, boolean completed) {}
}
