package fit.hutech.BuiBaoHan.controllers.api;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fit.hutech.BuiBaoHan.dto.ApiResponse;
import fit.hutech.BuiBaoHan.dto.PaymentRequest;
import fit.hutech.BuiBaoHan.entities.Order;
import fit.hutech.BuiBaoHan.entities.User;
import fit.hutech.BuiBaoHan.security.AuthResolver;
import fit.hutech.BuiBaoHan.services.OrderService;
import fit.hutech.BuiBaoHan.services.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST API Controller for Payment processing
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class ApiPaymentController {

    private final PaymentService paymentService;
    private final OrderService orderService;
    private final AuthResolver authResolver;

    /**
     * Create payment for order
     */
    @PostMapping("/create")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PaymentResponse>> createPayment(
            @AuthenticationPrincipal Object principal,
            @Valid @RequestBody PaymentRequest request,
            HttpServletRequest httpRequest) {
        try {
            User user = authResolver.resolveUser(principal);
            // Verify order belongs to user
            Order order = orderService.findByIdAndUser(request.orderId(), user)
                    .orElseThrow(() -> new IllegalArgumentException("Order not found"));
            
            // Create payment URL based on method
            PaymentResponse response = switch (request.paymentMethod()) {
                case VNPAY -> createVNPayPayment(order, httpRequest);
                case MOMO -> createMoMoPayment(order, httpRequest);
                case ZALOPAY -> createZaloPayPayment(order, httpRequest);
                case COD -> processCODPayment(order);
                default -> throw new IllegalArgumentException("Unsupported payment method: " + request.paymentMethod());
            };
            
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * VNPay callback
     */
    @GetMapping("/vnpay/callback")
    public ResponseEntity<ApiResponse<PaymentCallbackResponse>> vnpayCallback(
            @RequestParam Map<String, String> params) {
        try {
            var result = paymentService.processVNPayCallback(params);
            return ResponseEntity.ok(ApiResponse.success(PaymentCallbackResponse.from(result)));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * VNPay IPN (Instant Payment Notification)
     */
    @GetMapping("/vnpay/ipn")
    public ResponseEntity<Map<String, String>> vnpayIPN(@RequestParam Map<String, String> params) {
        try {
            paymentService.processVNPayIPN(params);
            return ResponseEntity.ok(Map.of("RspCode", "00", "Message", "Confirm Success"));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("RspCode", "99", "Message", e.getMessage()));
        }
    }

    /**
     * MoMo callback
     */
    @PostMapping("/momo/callback")
    public ResponseEntity<ApiResponse<PaymentCallbackResponse>> momoCallback(
            @RequestBody Map<String, Object> payload) {
        try {
            var result = paymentService.processMoMoCallback(payload);
            return ResponseEntity.ok(ApiResponse.success(PaymentCallbackResponse.from(result)));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * ZaloPay callback
     */
    @PostMapping("/zalopay/callback")
    public ResponseEntity<Map<String, Object>> zaloPayCallback(
            @RequestBody Map<String, Object> payload) {
        try {
            paymentService.processZaloPayCallback(payload);
            return ResponseEntity.ok(Map.of("return_code", 1, "return_message", "success"));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("return_code", 0, "return_message", e.getMessage()));
        }
    }

    /**
     * Check payment status
     */
    @GetMapping("/status/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PaymentStatusResponse>> getPaymentStatus(
            @AuthenticationPrincipal Object principal,
            @PathVariable Long orderId) {
        User user = authResolver.resolveUser(principal);
        return orderService.findByIdAndUser(orderId, user)
                .map(order -> {
                    PaymentStatusResponse response = new PaymentStatusResponse(
                            order.getId(),
                            order.getOrderCode(),
                            order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null,
                            order.getPaymentStatus() != null ? order.getPaymentStatus().name() : "PENDING",
                            order.getPaidDate() != null ? order.getPaidDate().toString() : null,
                            order.getTransactionId()
                    );
                    return ResponseEntity.ok(ApiResponse.success(response));
                })
                .orElse(ResponseEntity.badRequest()
                        .body(ApiResponse.notFound("Order")));
    }

    /**
     * Request refund
     */
    @PostMapping("/refund/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<RefundResponse>> requestRefund(
            @AuthenticationPrincipal Object principal,
            @PathVariable Long orderId,
            @RequestParam(required = false) String reason) {
        try {
            User user = authResolver.resolveUser(principal);
            Order order = orderService.findByIdAndUser(orderId, user)
                    .orElseThrow(() -> new IllegalArgumentException("Order not found"));
            
            var result = paymentService.requestRefund(order, reason);
            var refundResponse = new RefundResponse(
                    result.orderId(),
                    result.status(),
                    result.refundId(),
                    result.message()
            );
            return ResponseEntity.ok(ApiResponse.success("Refund requested", refundResponse));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    // ==================== Private Helper Methods ====================

    private PaymentResponse createVNPayPayment(Order order, HttpServletRequest request) {
        String clientIP = getClientIP(request);
        String paymentUrl = paymentService.createVNPayPaymentUrl(order, clientIP);
        return new PaymentResponse("VNPAY", paymentUrl, null, "PENDING");
    }

    private PaymentResponse createMoMoPayment(Order order, @SuppressWarnings("unused") HttpServletRequest request) {
        String paymentUrl = paymentService.createMoMoPaymentUrl(order);
        return new PaymentResponse("MOMO", paymentUrl, null, "PENDING");
    }

    private PaymentResponse createZaloPayPayment(Order order, @SuppressWarnings("unused") HttpServletRequest request) {
        String paymentUrl = paymentService.createZaloPayPaymentUrl(order);
        return new PaymentResponse("ZALOPAY", paymentUrl, null, "PENDING");
    }

    private PaymentResponse processCODPayment(Order order) {
        paymentService.processCODPayment(order);
        return new PaymentResponse("COD", null, null, "COD_PENDING");
    }

    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    // ==================== Inner Records ====================

    public record PaymentResponse(
            String paymentMethod,
            String paymentUrl,
            String qrCode,
            String status
    ) {}

    public record PaymentCallbackResponse(
            Long orderId,
            String orderNumber,
            boolean success,
            String message,
            String transactionId
    ) {
        public static PaymentCallbackResponse from(PaymentService.PaymentResult result) {
            return new PaymentCallbackResponse(
                    result.orderId(),
                    result.orderNumber(),
                    result.success(),
                    result.message(),
                    result.transactionId()
            );
        }
    }

    public record PaymentStatusResponse(
            Long orderId,
            String orderNumber,
            String paymentMethod,
            String paymentStatus,
            String paidAt,
            String transactionId
    ) {}

    public record RefundResponse(
            Long orderId,
            String status,
            String refundId,
            String message
    ) {}
}
